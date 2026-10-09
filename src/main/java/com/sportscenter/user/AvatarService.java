package com.sportscenter.user;

import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.media.MediaAsset;
import com.sportscenter.media.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AvatarService {
    private final UserRepository userRepository;
    private final MediaService mediaService;

    @Transactional
    public AvatarResponse upload(String email, MultipartFile file) {
        User user = userRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String previousPublicId = user.getAvatarPublicId();
        MediaAsset asset = mediaService.uploadAvatar(file);
        user.setAvatarUrl(asset.url());
        user.setAvatarPublicId(asset.publicId());
        userRepository.save(user);
        if (previousPublicId != null && !previousPublicId.equals(asset.publicId())) {
            try {
                mediaService.delete(previousPublicId);
            } catch (RuntimeException ignored) {
                // Keep the new avatar active even if cleanup of the old remote asset fails.
            }
        }
        return new AvatarResponse(asset.url());
    }

    @Transactional
    public void delete(String email) {
        User user = userRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String previousPublicId = user.getAvatarPublicId();
        user.setAvatarUrl(null);
        user.setAvatarPublicId(null);
        userRepository.save(user);
        if (previousPublicId != null) mediaService.delete(previousPublicId);
    }
}
