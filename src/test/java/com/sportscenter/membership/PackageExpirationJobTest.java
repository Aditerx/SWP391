package com.sportscenter.membership;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackageExpirationJobTest {
    @org.mockito.Spy private java.time.Clock clock = java.time.Clock.system(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private MemberPackageService memberPackageService;

    @InjectMocks
    private PackageExpirationJob packageExpirationJob;

    @Test
    @DisplayName("scanExpiredPackages gọi memberPackageService.expireOutdatedPackages thành công")
    void scanExpiredPackages_Success() {
        when(memberPackageService.expireOutdatedPackages()).thenReturn(3);

        packageExpirationJob.scanExpiredPackages();

        verify(memberPackageService).expireOutdatedPackages();
    }

    @Test
    @DisplayName("scanExpiredPackages an toàn không văng lỗi khi service ném ngoại lệ")
    void scanExpiredPackages_CatchesExceptionSafely() {
        when(memberPackageService.expireOutdatedPackages()).thenThrow(new RuntimeException("DB Connection timeout"));

        assertDoesNotThrow(() -> packageExpirationJob.scanExpiredPackages());
    }
}

