package home.artem.rates.rate;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {
  List<CurrencyRate> findByDateBetween(LocalDate dateFrom, LocalDate dateTo);

  List<CurrencyRate> findByDateBetweenAndToCurrencyLetterCodeIn(
      LocalDate dateFrom, LocalDate dateTo, List<String> currencies);
}
