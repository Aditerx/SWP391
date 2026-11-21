package com.sportscenter.normal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
@RestController @RequestMapping("/api/normal-refunds") @RequiredArgsConstructor
public class NormalRefundController {
    private final NormalRefundService service;
    @GetMapping public List<NormalRefundService.View> list(Authentication auth){return service.list(auth);}
    @GetMapping("/{id}") public NormalRefundService.View get(@PathVariable Integer id,Authentication auth){return service.get(id,auth);}
    @GetMapping("/{id}/history") public NormalRefundService.History history(@PathVariable Integer id,Authentication auth){return service.history(id,auth);}
    @PostMapping("/{id}/receiver") public NormalRefundService.View receiver(@PathVariable Integer id,@RequestBody NormalRefundService.ReceiverInput body,Authentication auth){return service.receiver(id,body,auth);}
    @PostMapping("/{id}/verify-receiver") public NormalRefundService.View verifyReceiver(@PathVariable Integer id,@RequestBody NormalRefundService.Review body,Authentication auth){return service.verifyReceiver(id,body,auth);}
    @PostMapping("/{id}/approve")
    @io.swagger.v3.oas.annotations.Operation(description="Active CenterManager with MANAGE_INVOICES in the same center. Requires current version, eligible refund and independently verified receiver. Approval is not a completed bank transfer.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Refund approved; no bank payment implied"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409", description="Conflict: reload current resource; body status is409 when present", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public NormalRefundService.View approve(@PathVariable Integer id,@RequestBody NormalRefundService.Review body,Authentication auth){return service.approve(id,body,auth);}
    @PostMapping("/{id}/claim") public NormalRefundService.AttemptView claim(@PathVariable Integer id,@RequestBody NormalRefundService.Claim body,Authentication auth){return service.claim(id,body,auth);}
    @PostMapping("/{id}/attempts/{attempt}/submit") public NormalRefundService.AttemptView submit(@PathVariable Integer id,@PathVariable Integer attempt,@RequestBody NormalRefundService.Submission body,Authentication auth){return service.submit(id,attempt,body,auth);}
    @PostMapping(value="/{id}/attempts/{attempt}/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Integer upload(@PathVariable Integer id,@PathVariable Integer attempt,@RequestPart("file") MultipartFile file,Authentication auth){return service.upload(id,attempt,file,auth);}
    @PostMapping("/{id}/attempts/{attempt}/verify") public NormalRefundService.View verify(@PathVariable Integer id,@PathVariable Integer attempt,@RequestBody NormalRefundService.Verification body,Authentication auth){return service.verify(id,attempt,body,auth);}
    @PostMapping("/{id}/issues") public NormalRefundService.View issue(@PathVariable Integer id,@RequestBody NormalRefundService.Review body,Authentication auth){return service.issue(id,body,auth);}
    @PostMapping("/{id}/request-information") public NormalRefundService.View requestInformation(@PathVariable Integer id,@RequestBody NormalRefundService.Review body,Authentication auth){return service.requestInformation(id,body,auth);}
    @GetMapping("/{id}/documents/{document}") public ResponseEntity<byte[]> download(@PathVariable Integer id,@PathVariable Integer document,Authentication auth){var d=service.download(id,document,auth);return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).header(HttpHeaders.CACHE_CONTROL,"no-store, private").header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=refund-proof-"+document).body(d.content());}
}
