package com.sportscenter.media;

import com.sportscenter.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class MediaService {
    private static final String AVATAR_TRANSFORMATION = "c_fill,h_256,w_256";

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;
    @Value("${cloudinary.api-key:}")
    private String apiKey;
    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public MediaAsset uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 2L * 1024 * 1024) {
            throw new BusinessException("Avatar must be an image no larger than 2 MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException("Could not read avatar file");
        }
        String extension = detectAllowedExtension(bytes);
        return upload("avatars", bytes, "image/" + extension, 2L * 1024 * 1024,
                Set.of("jpg", "png", "webp"), AVATAR_TRANSFORMATION);
    }

    public MediaAsset upload(String folder, byte[] bytes, String contentType, long maxBytes,
                             Set<String> allowedTypes, String transformation) {
        if (bytes == null || bytes.length == 0 || bytes.length > maxBytes) {
            throw new BusinessException("File size is outside the allowed range");
        }
        String extension = contentType == null ? "" : contentType.substring(contentType.lastIndexOf('/') + 1);
        if (!allowedTypes.contains(extension)) throw new BusinessException("Unsupported file type");
        requireConfiguration();

        String publicId = folder.replaceAll("^/+|/+$", "") + "/" + UUID.randomUUID();
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> signed = new TreeMap<>();
        signed.put("public_id", publicId);
        signed.put("timestamp", Long.toString(timestamp));
        if (transformation != null && !transformation.isBlank()) signed.put("transformation", transformation);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(bytes) {
            @Override public String getFilename() { return "avatar." + extension; }
        });
        form.add("api_key", apiKey);
        form.add("timestamp", Long.toString(timestamp));
        form.add("public_id", publicId);
        if (transformation != null && !transformation.isBlank()) form.add("transformation", transformation);
        form.add("signature", signature(signed));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        try {
            Map<?, ?> response = restTemplate.postForObject(
                    "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload",
                    new HttpEntity<>(form, headers), Map.class);
            if (response == null || response.get("secure_url") == null) {
                throw new BusinessException("Image upload failed");
            }
            return new MediaAsset(response.get("secure_url").toString(), publicId);
        } catch (RestClientException exception) {
            throw new BusinessException("Image upload failed");
        }
    }

    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        requireConfiguration();
        long timestamp = Instant.now().getEpochSecond();
        Map<String, String> signed = new TreeMap<>();
        signed.put("public_id", publicId);
        signed.put("timestamp", Long.toString(timestamp));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("public_id", publicId);
        form.add("timestamp", Long.toString(timestamp));
        form.add("api_key", apiKey);
        form.add("signature", signature(signed));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            restTemplate.postForObject("https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy",
                    new HttpEntity<>(form, headers), Map.class);
        } catch (RestClientException exception) {
            throw new BusinessException("Could not remove the previous image");
        }
    }

    private String detectAllowedExtension(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) return "jpg";
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P'
                && bytes[2] == 'N' && bytes[3] == 'G') return "png";
        if (bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) return "webp";
        throw new BusinessException("Only JPG, PNG, and WEBP images are supported");
    }

    private String signature(Map<String, String> parameters) {
        String canonical = parameters.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right).orElse("") + apiSecret;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 is unavailable", exception);
        }
    }

    private void requireConfiguration() {
        if (cloudName == null || cloudName.isBlank() || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new BusinessException("Avatar storage is not configured");
        }
    }
}
