package com.sportscenter.normal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NormalProposalRepository extends JpaRepository<NormalProposal,Integer> {
    List<NormalProposal> findByClassIdOrderByIdDesc(Integer id);
}
