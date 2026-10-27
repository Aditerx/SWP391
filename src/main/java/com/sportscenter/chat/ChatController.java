package com.sportscenter.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/chat") @RequiredArgsConstructor
public class ChatController {
    private final ChatService service;
    @PostMapping("/conversations")
    @PreAuthorize("hasRole('MEMBER') and hasAuthority('CHAT_AS_MEMBER')")
    public ResponseEntity<ChatConversationResponse> getOrCreate(@RequestBody(required=false) ChatConversationRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.OK).body(service.getOrCreate(auth,request));
    }
    @GetMapping("/conversations/me/active") @PreAuthorize("hasRole('MEMBER') and hasAuthority('CHAT_AS_MEMBER')")
    public ChatConversationResponse active(Authentication auth) { return service.active(auth); }
    @GetMapping("/me/unread-count") @PreAuthorize("hasRole('MEMBER') and hasAuthority('CHAT_AS_MEMBER')")
    public ChatUnreadCountResponse unread(Authentication auth) { return service.memberUnread(auth); }
    @GetMapping("/conversations/{id}/messages")
    public List<ChatMessageResponse> messages(@PathVariable Integer id,@RequestParam(defaultValue="0") Integer afterId,
            @RequestParam(defaultValue="30") Integer limit,Authentication auth) { return service.listMessages(id,afterId,limit,auth); }
    @PostMapping("/conversations/{id}/messages")
    public ChatMessageResponse send(@PathVariable Integer id,@Valid @RequestBody ChatMessageRequest request,Authentication auth) { return service.sendMessage(id,request,auth); }
    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<Void> read(@PathVariable Integer id,Authentication auth) { service.markRead(id,auth); return ResponseEntity.noContent().build(); }
    @GetMapping("/conversations") @PreAuthorize("hasAuthority('HANDLE_CHAT')")
    public Page<ChatConversationResponse> inbox(@RequestParam(required=false) ChatConversationStatus status,
            @RequestParam(required=false) Boolean assigned,@RequestParam(defaultValue="false") boolean unreadOnly,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="30") int size,
            Authentication auth) { return service.inbox(status,assigned,unreadOnly,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(100,size))),auth); }
    @PostMapping("/conversations/{id}/claim") @PreAuthorize("hasAuthority('HANDLE_CHAT')")
    public ChatConversationResponse claim(@PathVariable Integer id,Authentication auth) { return service.claim(id,auth); }
    @PostMapping("/conversations/{id}/release") @PreAuthorize("hasAuthority('HANDLE_CHAT')")
    public ChatConversationResponse release(@PathVariable Integer id,Authentication auth) { return service.release(id,auth); }
    @PatchMapping("/conversations/{id}/close") @PreAuthorize("hasAuthority('HANDLE_CHAT')")
    public ChatConversationResponse close(@PathVariable Integer id,Authentication auth) { return service.close(id,auth); }
    @PatchMapping("/conversations/{id}/reopen") @PreAuthorize("hasAuthority('HANDLE_CHAT')")
    public ChatConversationResponse reopen(@PathVariable Integer id,Authentication auth) { return service.reopen(id,auth); }
}
