package org.example.fileconversionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

////входящее сообщение
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileConversionRequest {
    private String messageId;
    private String filePath;

}
