package com.sportscenter.media;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AvatarUrlOptimizerTest {
    @Test void insertsTransformAfterUploadSegment() {
        assertEquals("https://res.cloudinary.com/demo/image/upload/f_auto,q_auto/avatar.jpg",
                AvatarUrlOptimizer.optimize("https://res.cloudinary.com/demo/image/upload/avatar.jpg"));
    }
    @Test void doesNotInsertWhenAutoFormatAlreadyExists() {
        String url = "https://res.cloudinary.com/demo/image/upload/f_auto,w_100/avatar.jpg";
        assertEquals(url, AvatarUrlOptimizer.optimize(url));
    }
    @Test void keepsExternalAndNullUrls() {
        String external = "https://example.com/avatar.jpg";
        assertEquals(external, AvatarUrlOptimizer.optimize(external));
        assertNull(AvatarUrlOptimizer.optimize(null));
    }
}
