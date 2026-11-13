package com.sportscenter.normal;
import com.sportscenter.sportclass.SportsClassResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;
@RestController @RequiredArgsConstructor
public class NormalChangeController {
    private final NormalChangeService service;
    @GetMapping("/api/classes/{id}/normal-proposals") public List<NormalChangeService.ProposalView> list(@PathVariable Integer id,Authentication auth){return service.list(id,auth);}
    @PostMapping("/api/classes/{id}/normal-proposals")
    @io.swagger.v3.oas.annotations.Operation(description="Current class lifecycleVersion required. NORMAL_VERSION_STALE is in message (no code field); HTTP and body status409. Active CenterManager with MANAGE_CLASSES and same center.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Proposal created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409", description="Conflict: reload current resource; body status is409 when present", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public NormalChangeService.ProposalView propose(@PathVariable Integer id,@RequestBody NormalChangeService.ProposalInput body,Authentication auth){return service.propose(id,body,auth);}
    @PostMapping("/api/normal-proposals/{id}/respond")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Member choice saved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409", description="Conflict: reload current resource; body status is409 when present", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public NormalChangeService.ProposalView respond(@PathVariable Integer id,@RequestBody NormalChangeService.Choice body,Authentication auth){return service.respond(id,body,auth);}
    @PostMapping("/api/normal-proposals/{id}/apply")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Proposal applied"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409", description="Conflict: reload current resource; body status is409 when present", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public NormalChangeService.ProposalView apply(@PathVariable Integer id,@RequestBody NormalChangeService.Decision body,Authentication auth){return service.apply(id,body,auth);}
    @PostMapping("/api/classes/{id}/cancel-before-opening")
    @io.swagger.v3.oas.annotations.Operation(description="Current class lifecycleVersion required; stale version returns409 without changes. Active CenterManager with MANAGE_CLASSES and same center.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Class cancelled before opening"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid input, state, time or workflow", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Authentication required", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Current role, authority, Active account or resource scope denied", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409", description="Conflict: reload current resource; body status is409 when present", content=@io.swagger.v3.oas.annotations.media.Content)
    })
    public SportsClassResponse cancel(@PathVariable Integer id,@RequestBody NormalChangeService.Decision body,Authentication auth){return service.cancel(id,body,auth);}
}
