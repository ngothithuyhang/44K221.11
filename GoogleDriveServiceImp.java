package mine.imageweb.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;


// phương thức lưu ảnh lên driver
@Service
public class GoogleDriveServiceImp implements GoogleDriveService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleDriveServiceImp.class);
	private static final String CREDENTIALS_FILE_PATH = "/secret.json";
	private static final String TOKENS_DIRECTORY_PATH = "tokens";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

	@Value("${google.service_account_email}")
	private String serviceAccountEmail;
	@Value("${google.application_name}")
	private String applicationName;
	@Value("${google.folder_id}")
	private String folderID;

	// kết nối vào driver
	public Drive getDriveService() {
		Drive service = null;

		// Build a new authorized API client service.
		try {
			final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName("ImageWeb").build();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return service;

	}

	private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveServiceImp.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8081).build(); // PORT URI OF GOOGLE SERVICE
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	@Override
	public File uploadFile(MultipartFile multipartFile, String mimeType) {

		File file = new File();
		try {
			com.google.api.services.drive.model.File fileMetadata = new File();
			fileMetadata.setName(multipartFile.getName());
			fileMetadata.setMimeType(mimeType);
			fileMetadata.setParents(Collections.singletonList(folderID));
			com.google.api.client.http.InputStreamContent inputStreamContent = new InputStreamContent(mimeType,
					multipartFile.getInputStream());
			file = getDriveService().files().create(fileMetadata, inputStreamContent)
					.setFields("id,webContentLink,webViewLink").execute();

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}

		return file;
	}

}
