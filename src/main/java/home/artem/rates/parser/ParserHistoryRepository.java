package home.artem.rates.parser;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParserHistoryRepository extends JpaRepository<ParserHistory, Date> {
	List<ParserHistory> findByDateBetween(LocalDate dateFrom, LocalDate dateTo);
}
