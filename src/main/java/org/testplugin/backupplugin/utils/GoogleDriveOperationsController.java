package org.testplugin.backupplugin.utils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import com.google.api.services.drive.model.FileList;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class GoogleDriveOperationsController {
    private static final String APPLICATION_NAME = "";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
            Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials_oauth.json";

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveOperationsController.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        //returns an authorized Credential object.
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

//    private static Drive buildDrive() {
//        ) {
//            ;
//            Drive service1 = Drive.Builder()
//        }
//    }

    public static void backup() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        Credential credential = getCredentials(HTTP_TRANSPORT);
//        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                .setApplicationName(APPLICATION_NAME)
//                .build();
        // ----------------
        InputStream in = GoogleDriveOperationsController.class.getResourceAsStream("/credentials.json");
//
//        GoogleCredentials credentials = GoogleCredentials.fromStream(in).createScoped(SCOPES);
//        HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(credentials);
//        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, adapter)
//                .setApplicationName(APPLICATION_NAME).build();
        // ----------------
//        FileInputStream credentialsStream = new FileInputStream();
        GoogleCredential credentials = GoogleCredential.fromStream(in)
                .createScoped(SCOPES);

        // Build the Google Drive service
        Drive service = new Drive.Builder(credentials.getTransport(), credentials.getJsonFactory(), credentials)
                .setApplicationName(APPLICATION_NAME)
                .build();
        // DELETE ALL
//        FileList fileList = service.files().list().execute();
//        for (File file : fileList.getFiles()) {
//            service.files().delete(file.getId()).execute();
//        }
//        service.files().emptyTrash();
        //
        Bukkit.getLogger().info("[DRIVER] Driver instance created.");
        java.io.File worldsFolder = Bukkit.getWorldContainer();
        java.io.File[] serverFiles = worldsFolder.listFiles();

        if (serverFiles == null) {
            Bukkit.getLogger().info("[ERROR] Can't find folders with worlds for server: " + Bukkit.getName());
            return;
        }
        List<java.io.File> worlds = new ArrayList<>(Arrays.asList(serverFiles));
        Predicate<java.io.File> isWorldFolder = file -> file.getName().startsWith("world");
        worlds = worlds.stream().filter(isWorldFolder).collect(Collectors.toList());
        File rootFolder;
        try {
            rootFolder = service.files().list().setQ("name='backups'").execute().getFiles().get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new FileNotFoundException("Backups folder not exists.");
        }
        Bukkit.getLogger().info("[WORLDS] Worlds container and folders obtained.");
        Bukkit.getLogger().info(rootFolder.getName());
        List<File> fileList = service.files().list().setQ("name='" + Bukkit.getServer().getName() + "'").execute().getFiles();
        Runnable task;
        Bukkit.getLogger().info("[UPLOAD-START] Start creating tasks.");
        for (java.io.File world : worlds) {
            Bukkit.getLogger().info("[TASK] Task for world: " + world.getName());
            task = new UploadFolderTask(service, world.getAbsolutePath(), rootFolder.getId());
            new Thread(task).start();
    }

}

    private static File prepareBackupSpace(Drive service, List<File> fileList, String rootFolderId) throws IOException {
        File folderOnDrive;
        Bukkit.getLogger().info("[INFO:PREPARE-SPACE] Start backup preparations");
        if (!fileList.isEmpty()) {
            Bukkit.getLogger().info(fileList.toString());
            Bukkit.getLogger().info(rootFolderId);
            folderOnDrive = fileList.get(0);
            Drive.Files.List request = service.files().list().setQ("'" + folderOnDrive.getId() + "' in parents");
            FileList filesDelete;
            do {
                filesDelete = request.execute();
                for (File file : filesDelete.getFiles()) {
                    service.files().delete(file.getId()).execute();
                }
                request.setPageToken(filesDelete.getNextPageToken());
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        } else {
            folderOnDrive = service.files().create(new File().setName(Bukkit.getServer().getName())
                    .setParents(Collections.singletonList(rootFolderId))
                    .setMimeType("application/vnd.google-apps.folder")).execute();
            Bukkit.getLogger().info(rootFolderId);
            Bukkit.getLogger().info(folderOnDrive.getName());
            Bukkit.getLogger().info(folderOnDrive.getParents().toString());
        }
        service.files().emptyTrash();
        Bukkit.getLogger().info("[INFO:PREPARE-SPACE] Backup space prepared successfully");
        return folderOnDrive;
    }

    public static void uploadDirectory(Drive driveService, String filePath, String parentFolderId) throws IOException {
        java.io.File folder = new java.io.File(filePath);

        if (!folder.isDirectory()) {
            System.out.println(folder);
            throw new IllegalArgumentException("Provided isn't a directory");

        }
        File folderMetadata = new File();
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(parentFolderId));
        folderMetadata.setName(folder.getName());
        File uploadedFolder = driveService.files().create(folderMetadata).setFields("id").execute();
        java.io.File[] directoryFiles = folder.listFiles();
        if (directoryFiles == null) {
            return;
        }
        for (java.io.File file : directoryFiles) {
            if (file.isDirectory()) {
                Bukkit.getLogger().info(file.getName());
                uploadDirectory(driveService, file.getAbsolutePath(), uploadedFolder.getId());
            } else {
                uploadFile(driveService, file.toPath(), uploadedFolder.getId());
            }
        }
    }

    public static void uploadFile(Drive driveService, Path filePath, String parentFolderId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(filePath.getFileName().toString());
        fileMetadata.setParents(Collections.singletonList(parentFolderId));
        FileContent mediaContent = new FileContent(Files.probeContentType(filePath), filePath.toFile());
        driveService.files().create(fileMetadata, mediaContent).execute();
    }
}

class UploadFolderTask implements Runnable {
    Drive drive;
    String directoryPath;
    String startFolderId;

    public UploadFolderTask(Drive drive, String directoryPath, String startFolderId) {
        this.drive = drive;
        this.directoryPath = directoryPath;
        this.startFolderId = startFolderId;
    }

    @Override
    public void run() {
        try {
            GoogleDriveOperationsController.uploadDirectory(this.drive, this.directoryPath, this.startFolderId);
            System.out.println("[TASK-SUCCESS] Success: " + this.directoryPath);
        } catch (IOException e) {
            System.err.println("[TASK-ERROR] Error while uploading directory: " + this.directoryPath);
        }
    }
}
