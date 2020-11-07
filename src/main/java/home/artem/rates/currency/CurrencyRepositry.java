package home.artem.rates.currency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepositry extends JpaRepository<Currency, String> {

}
