package com.sportscenter.chat.ai.tool;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.invoice.InvoiceRepository;
import com.sportscenter.invoice.Invoice;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.session.Session;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.room.Room;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.subject.SubjectRepository;
import java.time.Clock;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class ScmsReadOnlyAiToolsTest {
    @Test void publicResultsRespectRowAndCharacterLimitsAndExcludeSensitiveFields() throws Exception {
        MembershipPackageRepository packages = mock(MembershipPackageRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        List<MembershipPackage> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            MembershipPackage item = new MembershipPackage();
            item.setName("Package " + i);
            item.setPrice(BigDecimal.valueOf(100_000));
            item.setDurationDays(30);
            item.setBenefits("B".repeat(1000));
            many.add(item);
        }
        when(packages.findByCenterIdAndStatusIgnoreCase(eq(1), eq("Active"), any())).thenReturn(many);
        ScmsReadOnlyAiTools service = new ScmsReadOnlyAiTools(packages, mock(SubjectRepository.class),
                mock(SportsClassRepository.class), mock(ClassEnrollmentRepository.class), mock(MemberPackageRepository.class),
                mock(SessionRepository.class), mock(InvoiceRepository.class), new ObjectMapper(), Clock.systemUTC(),
                new com.sportscenter.chat.ai.ChatAiProperties(), tx);
        AiTool tool = service.all().stream().filter(item -> item.name().equals("get_membership_packages")).findFirst().orElseThrow();

        String result = tool.execute(new ToolContext(null), new ObjectMapper().createObjectNode());

        assertTrue(result.length() <= 2000);
        assertTrue(new ObjectMapper().readTree(result).size() <= 10);
        assertTrue(result.contains("\"price\":100000"));
        assertFalse(result.contains("email"));
        assertFalse(result.contains("memberId"));
        verify(packages).findByCenterIdAndStatusIgnoreCase(eq(1), eq("Active"), argThat(page -> page.getPageSize() == 10));
    }

    @Test void memberToolUsesOnlyContextAndReturnsNarrowDtos() {
        MemberPackageRepository subscriptions = mock(MemberPackageRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(subscriptions.findRecentByMemberId(eq(101), any())).thenReturn(List.of(subscription("A private plan")));
        when(subscriptions.findRecentByMemberId(eq(202), any())).thenReturn(List.of(subscription("B private plan")));
        ScmsReadOnlyAiTools service = new ScmsReadOnlyAiTools(mock(MembershipPackageRepository.class),
                mock(SubjectRepository.class), mock(SportsClassRepository.class), mock(ClassEnrollmentRepository.class),
                subscriptions, mock(SessionRepository.class), mock(InvoiceRepository.class), new ObjectMapper(),
                Clock.systemUTC(), new com.sportscenter.chat.ai.ChatAiProperties(), tx);
        AiTool tool = service.all().stream().filter(item -> item.name().equals("get_my_subscriptions")).findFirst().orElseThrow();

        String a = tool.execute(new ToolContext(101), new ObjectMapper().createObjectNode());
        String b = tool.execute(new ToolContext(202), new ObjectMapper().createObjectNode());

        assertTrue(a.contains("A private plan"));
        assertFalse(a.contains("B private plan"));
        assertTrue(b.contains("B private plan"));
        assertFalse(b.contains("A private plan"));
        assertFalse(a.toLowerCase().contains("email"));
        assertFalse(tool.parametersSchema().toString().contains("memberId"));
        verify(subscriptions).findRecentByMemberId(eq(101), any());
        verify(subscriptions).findRecentByMemberId(eq(202), any());
        AiToolRegistry registry = new AiToolRegistry(service);
        assertThrows(IllegalArgumentException.class, () -> registry.require("model_generated_sql"));
    }

    @Test void rejectsModelSuppliedMemberIdAndUnknownArguments() {
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        ScmsReadOnlyAiTools service = new ScmsReadOnlyAiTools(mock(MembershipPackageRepository.class),
                mock(SubjectRepository.class), mock(SportsClassRepository.class), mock(ClassEnrollmentRepository.class),
                mock(MemberPackageRepository.class), mock(SessionRepository.class), mock(InvoiceRepository.class),
                new ObjectMapper(), Clock.systemUTC(), new com.sportscenter.chat.ai.ChatAiProperties(), tx);
        AiTool tool = service.all().stream().filter(item -> item.name().equals("get_my_subscriptions")).findFirst().orElseThrow();
        var args = new ObjectMapper().createObjectNode().put("memberId", 202);
        assertThrows(IllegalArgumentException.class, () -> tool.execute(new ToolContext(101), args));
    }

    @Test void openClassesUsesUnfilteredQueryForMissingAndBlankSubject() {
        SportsClassRepository classes = mock(SportsClassRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        AiTool tool = new ScmsReadOnlyAiTools(mock(MembershipPackageRepository.class), mock(SubjectRepository.class), classes,
                mock(ClassEnrollmentRepository.class), mock(MemberPackageRepository.class), mock(SessionRepository.class),
                mock(InvoiceRepository.class), new ObjectMapper(), Clock.systemUTC(), new com.sportscenter.chat.ai.ChatAiProperties(), tx)
                .all().stream().filter(item -> item.name().equals("get_open_classes")).findFirst().orElseThrow();

        tool.execute(new ToolContext(null), new ObjectMapper().createObjectNode());
        tool.execute(new ToolContext(null), new ObjectMapper().createObjectNode().put("subjectName", "  "));

        verify(classes, times(2)).findPublicOpenClasses(any());
        verify(classes, never()).findPublicOpenClassesBySubjectName(anyString(), any());
    }

    @Test void openClassesTrimsAndLowercasesSubjectBeforeFilteredQuery() {
        SportsClassRepository classes = mock(SportsClassRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        AiTool tool = new ScmsReadOnlyAiTools(mock(MembershipPackageRepository.class), mock(SubjectRepository.class), classes,
                mock(ClassEnrollmentRepository.class), mock(MemberPackageRepository.class), mock(SessionRepository.class),
                mock(InvoiceRepository.class), new ObjectMapper(), Clock.systemUTC(), new com.sportscenter.chat.ai.ChatAiProperties(), tx)
                .all().stream().filter(item -> item.name().equals("get_open_classes")).findFirst().orElseThrow();

        tool.execute(new ToolContext(null), new ObjectMapper().createObjectNode().put("subjectName", "  TENNIS  "));

        verify(classes).findPublicOpenClassesBySubjectName(eq("tennis"), any());
        verify(classes, never()).findPublicOpenClasses(any());
    }

    @Test void upcomingSessionsAcceptsValidRangeAndRejectsInvalidRanges() throws Exception {
        SessionRepository sessions = mock(SessionRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(sessions.findUpcomingForMember(eq(101), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(sessions.countUpcomingForMember(eq(101), any(), any(), any(), any())).thenReturn(0L);
        ScmsReadOnlyAiTools service = tools(sessions, mock(InvoiceRepository.class), tx);
        AiTool tool = service.all().stream().filter(item -> item.name().equals("get_my_upcoming_sessions")).findFirst().orElseThrow();
        ObjectMapper mapper = new ObjectMapper();
        var valid = mapper.createObjectNode().put("fromDate", "2026-11-16").put("toDate", "2026-12-16");
        var result = mapper.readTree(tool.execute(new ToolContext(101), valid));
        assertFalse(result.path("truncated").asBoolean());
        assertEquals(0, result.path("total").asInt());
        verify(sessions).findUpcomingForMember(eq(101), eq(LocalDate.parse("2026-11-16")), eq(LocalDate.parse("2026-12-16")), any(), any(), any());
        for (String[] dates : List.of(new String[]{"2026/11/16", "2026-11-20"},
                new String[]{"2026-11-20", "2026-11-19"}, new String[]{"2026-11-01", "2026-12-02"},
                new String[]{"2026-02-30", "2026-03-02"})) {
            var invalid = mapper.createObjectNode().put("fromDate", dates[0]).put("toDate", dates[1]);
            assertThrows(IllegalArgumentException.class, () -> tool.execute(new ToolContext(101), invalid));
        }
        verify(sessions, times(1)).findUpcomingForMember(anyInt(), any(), any(), any(), any(), any());
    }

    @Test void upcomingMarksTruncatedWhenMoreRowsExistThanLimit() throws Exception {
        SessionRepository sessions = mock(SessionRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        SportsClass sportsClass = new SportsClass(); sportsClass.setName("Cầu lông");
        Room room = new Room(); room.setName("Sân 1");
        List<Session> rows = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            Session session = new Session(); session.setSportsClass(sportsClass); session.setRoom(room);
            session.setSessionDate(LocalDate.of(2026, 11, 20)); session.setStartTime(LocalTime.of(9, 0)); session.setEndTime(LocalTime.of(10, 0)); rows.add(session);
        }
        when(sessions.findUpcomingForMember(eq(101), any(), any(), any(), any(), any())).thenReturn(rows);
        when(sessions.countUpcomingForMember(eq(101), any(), any(), any(), any())).thenReturn(11L);
        AiTool tool = tools(sessions, mock(InvoiceRepository.class), tx).all().stream()
                .filter(item -> item.name().equals("get_my_upcoming_sessions")).findFirst().orElseThrow();
        var result = new ObjectMapper().readTree(tool.execute(new ToolContext(101), new ObjectMapper().createObjectNode()));
        assertTrue(result.path("truncated").asBoolean());
        assertEquals(10, result.path("sessions").size());
        assertEquals(11, result.path("total").asInt());
        assertTrue(result.toString().length() <= 2000);
        verify(sessions).findUpcomingForMember(eq(101), eq(LocalDate.parse("2026-11-12")), eq(LocalDate.parse("2026-12-12")),
                eq(LocalDate.parse("2026-11-12")), any(), any());
    }

    @Test void thisWeekIsComputedInJavaAndOverridesModelDates() throws Exception {
        SessionRepository sessions = mock(SessionRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(sessions.findUpcomingForMember(eq(101), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(sessions.countUpcomingForMember(eq(101), any(), any(), any(), any())).thenReturn(0L);
        AiTool tool = tools(sessions, mock(InvoiceRepository.class), tx).all().stream()
                .filter(item -> item.name().equals("get_my_upcoming_sessions")).findFirst().orElseThrow();

        tool.execute(new ToolContext(101, "Tuần này tôi có buổi học nào?"),
                new ObjectMapper().createObjectNode().put("fromDate", "2026-11-01").put("toDate", "2026-11-30"));

        verify(sessions).findUpcomingForMember(eq(101), eq(LocalDate.parse("2026-11-09")), eq(LocalDate.parse("2026-11-15")),
                eq(LocalDate.parse("2026-11-12")), any(), any());
        verify(sessions).countUpcomingForMember(eq(101), eq(LocalDate.parse("2026-11-09")), eq(LocalDate.parse("2026-11-15")),
                eq(LocalDate.parse("2026-11-12")), any());
    }

    @Test void allSixToolDescriptionsHaveUseGuidanceAndPersonalToolsHaveExamples() {
        ScmsReadOnlyAiTools service = tools(mock(SessionRepository.class), mock(InvoiceRepository.class), mock(PlatformTransactionManager.class));
        assertEquals(6, service.all().size());
        for (AiTool tool : service.all()) assertTrue(tool.description().contains("Dùng khi người dùng hỏi về"), tool.name());
        for (String name : List.of("get_my_subscriptions", "get_my_upcoming_sessions", "get_my_recent_invoices")) {
            AiTool tool = service.all().stream().filter(item -> item.name().equals(name)).findFirst().orElseThrow();
            assertTrue(tool.description().contains("CHÍNH HỌ"));
            assertTrue(tool.description().contains("Ví dụ:"));
        }
    }

    @Test void recentInvoicesExposeNewestByDisplayDateNotInvoiceId() throws Exception {
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        Invoice olderHighCode = new Invoice(); olderHighCode.setInvoiceId(900); olderHighCode.setInvoiceCode("INV-900");
        olderHighCode.setPaymentDate(java.time.LocalDateTime.parse("2026-01-01T09:00:00"));
        Invoice newestLowCode = new Invoice(); newestLowCode.setInvoiceId(100); newestLowCode.setInvoiceCode("INV-100");
        newestLowCode.setPaymentDate(java.time.LocalDateTime.parse("2026-02-01T09:00:00"));
        when(invoices.findLatestByMemberForAi(eq(101), any())).thenReturn(List.of(olderHighCode, newestLowCode));
        AiTool tool = tools(mock(SessionRepository.class), invoices, tx).all().stream()
                .filter(item -> item.name().equals("get_my_recent_invoices")).findFirst().orElseThrow();
        String output = tool.execute(new ToolContext(101), new ObjectMapper().createObjectNode());
        assertTrue(output.indexOf("INV-100") < output.indexOf("INV-900"));
        assertTrue(output.contains("tối đa 5 hóa đơn mới nhất"));
        verify(invoices).findLatestByMemberForAi(eq(101), argThat(page -> page.getPageSize() == 5));
    }

    private ScmsReadOnlyAiTools tools(SessionRepository sessions, InvoiceRepository invoices, PlatformTransactionManager tx) {
        when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        return new ScmsReadOnlyAiTools(mock(MembershipPackageRepository.class), mock(SubjectRepository.class),
                mock(SportsClassRepository.class), mock(ClassEnrollmentRepository.class), mock(MemberPackageRepository.class),
                sessions, invoices, new ObjectMapper().findAndRegisterModules(), Clock.fixed(java.time.Instant.parse("2026-11-12T04:00:00Z"), java.time.ZoneId.of("Asia/Ho_Chi_Minh")),
                new com.sportscenter.chat.ai.ChatAiProperties(), tx);
    }

    private MemberPackage subscription(String name) {
        MembershipPackage pkg = new MembershipPackage();
        pkg.setName(name);
        MemberPackage item = new MemberPackage();
        item.setMembershipPackage(pkg);
        item.setStatus("Active");
        return item;
    }
}
