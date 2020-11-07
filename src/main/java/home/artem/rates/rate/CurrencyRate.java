package home.artem.rates.rate;

import java.time.LocalDate;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import home.artem.rates.currency.Currency;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"fromCurrencyId", "toCurrencyId", "date"})
    })
public class CurrencyRate {
  @Id @GeneratedValue private Long id;

  private LocalDate date;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "fromCurrencyId")
  private Currency fromCurrency;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "toCurrencyId")
  private Currency toCurrency;

  private Double value;
}
