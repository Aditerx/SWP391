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
    @PostMapping("/api/classes/{id}/normal-proposals") public NormalChangeService.ProposalView propose(@PathVariable Integer id,@RequestBody NormalChangeService.ProposalInput body,Authentication auth){return service.propose(id,body,auth);}
    @PostMapping("/api/normal-proposals/{id}/respond") public NormalChangeService.ProposalView respond(@PathVariable Integer id,@RequestBody NormalChangeService.Choice body,Authentication auth){return service.respond(id,body,auth);}
    @PostMapping("/api/normal-proposals/{id}/apply") public NormalChangeService.ProposalView apply(@PathVariable Integer id,@RequestBody NormalChangeService.Decision body,Authentication auth){return service.apply(id,body,auth);}
    @PostMapping("/api/classes/{id}/cancel-before-opening") public SportsClassResponse cancel(@PathVariable Integer id,@RequestBody NormalChangeService.Decision body,Authentication auth){return service.cancel(id,body,auth);}
}
