package mine.imageweb.service;


import org.springframework.web.multipart.MultipartFile;

import com.google.api.services.drive.model.File;

public interface GoogleDriveService {
	public File uploadFile( MultipartFile multipartFile, String mimeType);
}
