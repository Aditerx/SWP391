package com.sportscenter.chat.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sportscenter.enrollment.ClassEnrollmentRepository;
import com.sportscenter.invoice.Invoice;
import com.sportscenter.invoice.InvoiceRepository;
import com.sportscenter.membership.MemberPackage;
import com.sportscenter.membership.MemberPackageRepository;
import com.sportscenter.membership.MembershipPackage;
import com.sportscenter.membership.MembershipPackageRepository;
import com.sportscenter.session.Session;
import com.sportscenter.session.SessionRepository;
import com.sportscenter.sportclass.SportsClass;
import com.sportscenter.sportclass.SportsClassRepository;
import com.sportscenter.subject.Subject;
import com.sportscenter.subject.SubjectRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Fixed, read-only SCMS functions exposed to the chat model. */
@Component
public class ScmsReadOnlyAiTools {
    private static final int MAX_RESULT_CHARS = 2000;
    private static final Pattern CONTROLS = Pattern.compile("\\p{Cc}");
    private final MembershipPackageRepository packages;
    private final SubjectRepository subjects;
    private final SportsClassRepository classes;
    private final ClassEnrollmentRepository enrollments;
    private final MemberPackageRepository subscriptions;
    private final SessionRepository sessions;
    private final InvoiceRepository invoices;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final com.sportscenter.chat.ai.ChatAiProperties limits;
    private final TransactionTemplate readOnly;
    private final List<AiTool> all;

    public ScmsReadOnlyAiTools(MembershipPackageRepository packages, SubjectRepository subjects,
            SportsClassRepository classes, ClassEnrollmentRepository enrollments,
            MemberPackageRepository subscriptions, SessionRepository sessions, InvoiceRepository invoices,
            ObjectMapper mapper, Clock clock, com.sportscenter.chat.ai.ChatAiProperties limits, PlatformTransactionManager txManager) {
        this.packages = packages; this.subjects = subjects; this.classes = classes;
        this.enrollments = enrollments; this.subscriptions = subscriptions; this.sessions = sessions;
        this.invoices = invoices; this.mapper = mapper; this.clock = clock; this.limits = limits;
        this.readOnly = new TransactionTemplate(txManager);
        this.readOnly.setReadOnly(true);
        this.readOnly.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        this.all = List.of(
            tool("get_membership_packages", "Dùng khi người dùng hỏi về các gói hội viên đang bán. Ví dụ: ‘Trung tâm có gói tập nào?’", emptySchema(), (ctx,args) -> packages()),
            tool("get_subjects", "Dùng khi người dùng hỏi về các bộ môn đang mở. Ví dụ: ‘Có những bộ môn nào?’", emptySchema(), (ctx,args) -> subjects()),
            tool("get_open_classes", "Dùng khi người dùng hỏi về lớp nhóm đang mở. Ví dụ: ‘Tìm lớp cầu lông đang tuyển sinh.’", subjectSchema(), (ctx,args) -> openClasses(args)),
            tool("get_my_subscriptions", "Dùng khi người dùng hỏi về gói tập của CHÍNH HỌ. Ví dụ: ‘Gói tập của tôi hết hạn ngày nào?’", emptySchema(), (ctx,args) -> mySubscriptions(ctx)),
            tool("get_my_upcoming_sessions", "Dùng khi người dùng hỏi về lịch học của CHÍNH HỌ. Ví dụ: ‘Tuần này tôi có buổi tập nào?’", sessionSchema(), (ctx,args) -> upcoming(ctx,args)),
            tool("get_my_recent_invoices", "Dùng khi người dùng hỏi về hóa đơn của CHÍNH HỌ. Ví dụ: ‘Hóa đơn gần nhất của tôi là ngày nào?’ Chỉ trả tối đa 5 hóa đơn mới nhất.", emptySchema(), (ctx,args) -> recentInvoices(ctx))
        );
    }

    public List<AiTool> all() { return all; }

    private AiTool tool(String name, String description, JsonNode schema, BiReadFunction fn) {
        return new AiTool() {
            @Override public String name() { return name; }
            @Override public String description() { return description; }
            @Override public JsonNode parametersSchema() { return schema.deepCopy(); }
            @Override public String execute(ToolContext context, JsonNode args) {
                validate(name, args);
                long started = System.nanoTime();
                Object value = readOnly.execute(status -> fn.apply(context, args));
                if (value == null) value = List.of();
                BoundedResult bounded = boundedJson(value);
                org.slf4j.LoggerFactory.getLogger(ScmsReadOnlyAiTools.class).info(
                        "AI_CHAT_TOOL name={} latencyMs={} rows={}", name,
                        (System.nanoTime() - started) / 1_000_000, bounded.rowCount());
                return bounded.json();
            }
        };
    }

    private List<Map<String,Object>> packages() {
        return packages.findByCenterIdAndStatusIgnoreCase(1, "Active", PageRequest.of(0, limits.getToolMaxRows())).stream().limit(limits.getToolMaxRows())
                .map(p -> row("name", clean(p.getName()), "price", p.getPrice(), "durationDays", p.getDurationDays(), "benefits", clean(p.getBenefits())))
                .toList();
    }
    private List<Map<String,Object>> subjects() {
        return subjects.findByStatusIgnoreCase("Active", PageRequest.of(0, limits.getToolMaxRows())).stream().limit(limits.getToolMaxRows())
                .map(s -> row("name", clean(s.getName()), "description", clean(s.getDescription()))).toList();
    }
    private List<Map<String,Object>> openClasses(JsonNode args) {
        String filter = args.hasNonNull("subjectName") ? args.path("subjectName").asText().trim() : null;
        if (filter != null && filter.isBlank()) filter = null;
        if (filter != null) filter = filter.toLowerCase(java.util.Locale.ROOT);
        var page = PageRequest.of(0, limits.getToolMaxRows());
        var found = filter == null ? classes.findPublicOpenClasses(page)
                : classes.findPublicOpenClassesBySubjectName(filter, page);
        return found.stream().limit(limits.getToolMaxRows()).map(c -> {
            long enrolled = enrollments.countBySportsClassIdAndStatusIn(c.getId(), List.of("Registered", "Pending"));
            int capacity = c.getMaxCapacity() == null ? 0 : c.getMaxCapacity();
            return row("name", clean(c.getName()), "subject", clean(c.getSubject().getName()),
                    "startDate", c.getStartDate(), "endDate", c.getEndDate(), "capacity", capacity,
                    "seatsRemaining", Math.max(0, capacity - (int) enrolled), "status", clean(c.getStatus()));
        }).toList();
    }
    private List<Map<String,Object>> mySubscriptions(ToolContext context) {
        int memberId = memberId(context);
        return subscriptions.findRecentByMemberId(memberId, PageRequest.of(0, limits.getToolMaxRows())).stream().limit(limits.getToolMaxRows()).map(mp -> {
            MembershipPackage p = mp.getMembershipPackage();
            String name = p != null ? p.getName() : mp.getSubject() != null ? mp.getSubject().getName() + " Advanced" : "Gói hội viên";
            return row("name", clean(name), "startDate", mp.getStartDate(), "endDate", mp.getEndDate(), "status", clean(mp.getStatus()));
        }).toList();
    }
    private Object upcoming(ToolContext context, JsonNode args) {
        int memberId = memberId(context);
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate;
        LocalDate toDate;
        if (asksForThisWeek(context)) {
            fromDate = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            toDate = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
        } else {
            fromDate = parseDate(args, "fromDate", today);
            toDate = parseDate(args, "toDate", fromDate.plusDays(30));
        }
        if (toDate.isBefore(fromDate) || ChronoUnit.DAYS.between(fromDate, toDate) >= 31)
            throw new IllegalArgumentException("invalid_date_range");
        LocalTime nowTime = LocalTime.now(clock);
        List<Session> found = sessions.findUpcomingForMember(memberId, fromDate, toDate, today, nowTime,
                PageRequest.of(0, limits.getToolMaxRows() + 1));
        boolean truncated = found.size() > limits.getToolMaxRows();
        List<Map<String,Object>> rows = found.stream().limit(limits.getToolMaxRows())
                .map(s -> row("date", s.getSessionDate(), "startTime", s.getStartTime(), "endTime", s.getEndTime(),
                        "className", clean(s.getSportsClass().getName()), "room", clean(s.getRoom().getName())))
                .toList();
        long total = sessions.countUpcomingForMember(memberId, fromDate, toDate, today, nowTime);
        return row("sessions", rows, "truncated", truncated || total > rows.size(), "total", total);
    }

    private boolean asksForThisWeek(ToolContext context) {
        if (context == null || context.memberQuestion() == null) return false;
        String normalized = java.text.Normalizer.normalize(context.memberQuestion(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("tuan nay") || normalized.contains("this week");
    }
    private Object recentInvoices(ToolContext context) {
        int memberId = memberId(context);
        List<Map<String,Object>> rows = invoices.findLatestByMemberForAi(memberId, PageRequest.of(0, 5)).stream()
                .sorted(java.util.Comparator.comparing((Invoice i) -> i.getPaymentDate() != null ? i.getPaymentDate() : i.getCreatedAt(),
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(Invoice::getInvoiceId, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .limit(5).map(i -> {
            LocalDateTime date = i.getPaymentDate() != null ? i.getPaymentDate() : i.getCreatedAt();
            return row("invoiceCode", clean(i.getInvoiceCode()), "amount", i.getAmount(),
                    "paymentMethod", clean(i.getPaymentMethod()), "status", clean(i.getPaymentStatus()), "date", date);
        }).toList();
        return row("note", "Danh sách chỉ gồm tối đa 5 hóa đơn mới nhất theo ngày thanh toán hoặc ngày tạo.", "invoices", rows);
    }

    private int memberId(ToolContext context) {
        if (context == null || context.memberUserId() == null || context.memberUserId() <= 0)
            throw new IllegalArgumentException("missing_member_context");
        return context.memberUserId();
    }
    private void validate(String tool, JsonNode args) {
        if (args == null || !args.isObject()) throw new IllegalArgumentException("invalid_arguments");
        if ("get_open_classes".equals(tool)) {
            if (args.size() > 1 || (args.has("subjectName") && (!args.get("subjectName").isTextual()
                    || args.get("subjectName").asText().trim().length() > 80)))
                throw new IllegalArgumentException("invalid_arguments");
        } else if ("get_my_upcoming_sessions".equals(tool)) {
            if (args.size() > 2) throw new IllegalArgumentException("invalid_arguments");
            for (String key : List.of("fromDate", "toDate")) if (args.has(key)
                    && (!args.get(key).isTextual() || !args.get(key).asText().matches("\\d{4}-\\d{2}-\\d{2}")))
                throw new IllegalArgumentException("invalid_date_format");
        } else if (!args.isEmpty()) throw new IllegalArgumentException("invalid_arguments");
    }
    private JsonNode emptySchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object").put("additionalProperties", false);
        schema.set("properties", mapper.createObjectNode());
        return schema;
    }
    private JsonNode subjectSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object").put("additionalProperties", false);
        schema.putObject("properties").putObject("subjectName").put("type", "string").put("maxLength", 80);
        return schema;
    }
    private JsonNode sessionSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object").put("additionalProperties", false);
        ObjectNode props = schema.putObject("properties");
        for (String name : List.of("fromDate", "toDate")) props.putObject(name).put("type", "string").put("format", "date").put("pattern", "^\\d{4}-\\d{2}-\\d{2}$");
        return schema;
    }
    private LocalDate parseDate(JsonNode args, String key, LocalDate fallback) {
        if (!args.hasNonNull(key)) return fallback;
        try { return LocalDate.parse(args.path(key).asText()); }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException("invalid_date_format"); }
    }
    private BoundedResult boundedJson(Object source) {
        Object value = source;
        while (true) {
            try {
                String json = mapper.writeValueAsString(value);
                if (json.length() <= MAX_RESULT_CHARS) return new BoundedResult(json, value instanceof List<?> list ? list.size() : 1);
                if (value instanceof List<?> list && !list.isEmpty()) value = new ArrayList<>(list.subList(0, list.size() - 1));
                else if (value instanceof Map<?,?> map && map.get("sessions") instanceof List<?> rows && !rows.isEmpty()) {
                    var reduced = new java.util.LinkedHashMap<String,Object>();
                    map.forEach((key, item) -> reduced.put(String.valueOf(key), item));
                    reduced.put("sessions", new ArrayList<>(rows.subList(0, rows.size() - 1)));
                    reduced.put("truncated", true);
                    value = reduced;
                } else return new BoundedResult("{}", 0);
            } catch (Exception ex) { throw new IllegalArgumentException("tool_result_unavailable"); }
        }
    }
    private String clean(String value) {
        if (value == null) return null;
        String result = CONTROLS.matcher(value).replaceAll("").replace('\r', ' ').trim();
        return result.length() > 240 ? result.substring(0, 240) : result;
    }
    private Map<String,Object> row(Object... fields) {
        java.util.LinkedHashMap<String,Object> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < fields.length; i += 2) result.put((String) fields[i], fields[i + 1]);
        return result;
    }
    private record BoundedResult(String json, int rowCount) { }
    @FunctionalInterface private interface BiReadFunction { Object apply(ToolContext context, JsonNode args); }
}
