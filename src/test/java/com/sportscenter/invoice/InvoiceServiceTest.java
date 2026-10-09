package com.sportscenter.invoice;

import com.sportscenter.audit.AuditService;
import com.sportscenter.common.exception.BusinessException;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.user.User;
import com.sportscenter.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MembershipPackageRepository membershipPackageRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InvoiceService invoiceService;

    private User sampleMember;
    private MembershipPackage samplePackage;

    @BeforeEach
    void setUp() {
        sampleMember = new User();
        sampleMember.setId(7);
        sampleMember.setEmail("member-a@example.com");
        sampleMember.setFullName("Hoàng Thị Oanh");

        samplePackage = new MembershipPackage();
        samplePackage.setId(2);
        samplePackage.setName("Gói Tiêu Chuẩn 3 Tháng");
        samplePackage.setPrice(BigDecimal.valueOf(1350000));
        samplePackage.setDurationDays(90);
    }

    @Test
    @DisplayName("Create invoice successfully")
    void createInvoice_Success() {
        User sampleReceptionist = new User();
        sampleReceptionist.setId(5);
        sampleReceptionist.setFullName("Đỗ Thị Mai");

        InvoiceRequest request = new InvoiceRequest(7, 2, 5, BigDecimal.valueOf(1350000), "BankTransfer", "Paid", "TXN-70001", "Registration");

        when(userRepository.findById(7)).thenReturn(Optional.of(sampleMember));
        when(userRepository.findById(5)).thenReturn(Optional.of(sampleReceptionist));
        when(membershipPackageRepository.findById(2)).thenReturn(Optional.of(samplePackage));

        Invoice saved = new Invoice();
        saved.setInvoiceId(101);
        saved.setMember(sampleMember);
        saved.setMembershipPackage(samplePackage);
        saved.setAmount(BigDecimal.valueOf(1350000));
        saved.setPaymentMethod("BankTransfer");
        saved.setPaymentStatus("Paid");
        saved.setGatewayTransactionRef("TXN-70001");

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(saved);

        InvoiceResponse response = invoiceService.createInvoice(request);

        assertNotNull(response);
        assertEquals(101, response.invoiceId());
        assertEquals("Hoàng Thị Oanh", response.memberName());
        assertEquals("BankTransfer", response.paymentMethod());
        assertEquals("Paid", response.paymentStatus());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Pay invoice successfully")
    void payInvoice_Success() {
        Invoice pendingInvoice = new Invoice();
        pendingInvoice.setInvoiceId(200);
        pendingInvoice.setMember(sampleMember);
        pendingInvoice.setAmount(BigDecimal.valueOf(500000));
        pendingInvoice.setPaymentStatus("Pending");

        when(invoiceRepository.findById(200)).thenReturn(Optional.of(pendingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        InvoicePaymentRequest payReq = new InvoicePaymentRequest("Cash", null, 6, "Paid at desk");
        InvoiceResponse response = invoiceService.payInvoice(200, payReq);

        assertNotNull(response);
        assertEquals("Paid", response.paymentStatus());
        assertEquals("Cash", response.paymentMethod());
        assertNotNull(response.gatewayTransactionRef());
    }

    @Test
    @DisplayName("Pay invoice throws exception if already paid")
    void payInvoice_AlreadyPaid_ThrowsException() {
        Invoice paidInvoice = new Invoice();
        paidInvoice.setInvoiceId(201);
        paidInvoice.setPaymentStatus("Paid");

        when(invoiceRepository.findById(201)).thenReturn(Optional.of(paidInvoice));

        InvoicePaymentRequest payReq = new InvoicePaymentRequest("Cash", null, null, null);
        assertThrows(BusinessException.class, () -> invoiceService.payInvoice(201, payReq));
    }

    @Test
    @DisplayName("Find all invoices with null-safe stream filtering")
    void findAll_Success() {
        Invoice invoice1 = new Invoice();
        invoice1.setInvoiceId(1);
        invoice1.setMember(sampleMember);
        invoice1.setMembershipPackage(samplePackage);
        invoice1.setPaymentStatus("Paid");
        invoice1.setAmount(BigDecimal.valueOf(1350000));
        invoice1.setPaymentMethod("BankTransfer");

        Invoice invoice2 = new Invoice();
        invoice2.setInvoiceId(2);
        invoice2.setMember(sampleMember);
        invoice2.setPaymentStatus("Pending");
        invoice2.setAmount(BigDecimal.valueOf(500000));
        invoice2.setPaymentMethod("Cash");

        when(invoiceRepository.findAllWithDetails()).thenReturn(java.util.List.of(invoice1, invoice2));

        var all = invoiceService.findAll(null, null, null, null, null, null, null, null);
        assertEquals(2, all.size());

        var filtered = invoiceService.findAll(null, null, null, "Paid", null, null, null, null);
        assertEquals(1, filtered.size());
        assertEquals(1, filtered.get(0).invoiceId());
    }

    @Test
    @DisplayName("Member invoice list is scoped to the authenticated user's ID")
    void findAll_MemberOnlySeesOwnInvoices() {
        Invoice ownInvoice = new Invoice();
        ownInvoice.setInvoiceId(1);
        ownInvoice.setMember(sampleMember);
        ownInvoice.setPaymentStatus("Paid");
        ownInvoice.setAmount(BigDecimal.valueOf(100));

        var authentication = new UsernamePasswordAuthenticationToken(
                "member-a@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        when(userRepository.findByEmailIgnoreCase("member-a@example.com")).thenReturn(Optional.of(sampleMember));
        when(invoiceRepository.findByMemberId(7)).thenReturn(List.of(ownInvoice));

        var invoices = invoiceService.findAll(null, null, null, null, null, null, null, authentication);

        assertEquals(1, invoices.size());
        assertEquals(1, invoices.get(0).invoiceId());
        verify(invoiceRepository).findByMemberId(7);
    }

    @Test
    @DisplayName("Member cannot request another member's invoices")
    void findAll_MemberCannotOverrideScopeWithQueryParameter() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "member-a@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        when(userRepository.findByEmailIgnoreCase("member-a@example.com")).thenReturn(Optional.of(sampleMember));

        assertThrows(AccessDeniedException.class,
                () -> invoiceService.findAll(8, null, null, null, null, null, null, authentication));
        verifyNoInteractions(invoiceRepository);
    }
}
