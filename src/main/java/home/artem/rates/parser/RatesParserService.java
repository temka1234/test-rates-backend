package home.artem.rates.parser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import home.artem.rates.currency.Currency;
import home.artem.rates.rate.CurrencyRate;
import home.artem.rates.rate.CurrencyRateRepository;


@Service
public class RatesParserService {
	Logger logger = LoggerFactory.getLogger(RatesParserService.class);
	
	private static final Currency defaultFromCurrency = new Currency("810", "RUB", "Российский рубль");
	
	@Autowired
	CurrencyRateRepository rateRepository;
	
	@Autowired
	ParserHistoryRepository historyRepository;
	
	@Autowired
	private PlatformTransactionManager transactionManager;
	
	@Autowired 
	private EntityManager entityManager;
	
	@Value("${parser.source.url}")
	private String sourceUrl;

	public RatesParserService() {
	}
	
	public void fillMissingDates(LocalDate dateFrom, LocalDate dateTo) {
    	List<LocalDate> missingDates = getMissingDates(dateFrom, dateTo);
    	if (missingDates.size() > 0) {
    		parseMissingDates(dateFrom, dateTo);
    	}
	}
	
	private synchronized void parseMissingDates(LocalDate dateFrom, LocalDate dateTo) {
		List<LocalDate> missingDates = getMissingDates(dateFrom, dateTo);
		if (missingDates.size() > 0) {
			List<CompletableFuture<List<CurrencyRate>>> futures = parseDates(missingDates);
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
		}
	}
	
	private List<LocalDate> getMissingDates(LocalDate dateFrom, LocalDate dateTo) {
    	Set<LocalDate> historyDates = historyRepository
			.findByDateBetween(dateFrom, dateTo)
			.stream()
			.map(item -> item.getDate())
			.collect(Collectors.toSet());

    	return dateFrom
			.datesUntil(dateTo.plusDays(1))
			.filter(date -> !historyDates.contains(date))
			.collect(Collectors.toList());
	}
	
	private List<CompletableFuture<List<CurrencyRate>>> parseDates(List<LocalDate> dates) {
		return dates
			.stream()
			.map(date -> parseDate(date))
			.collect(Collectors.toList());
	}
	
	@Async
	private CompletableFuture<List<CurrencyRate>> parseDate(LocalDate date) {
	    logger.info("[{}] Start page parsing", date);
		try { 
			String requestDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
			String requestUrl = String.format("%s?UniDbQuery.Posted=True&UniDbQuery.To=%s", sourceUrl, requestDate);
			List<CurrencyRate> data = Jsoup.connect(requestUrl).get().select(".data tr")
				.stream()
				.skip(1)
				.map((tr) -> {
					List<String> tds = tr
						.select("td")
						.stream()
						.map((td) -> td.text())
						.collect(Collectors.toList());
					
					if (tds.size() == 5) {				
						Currency toCurrency = new Currency(tds.get(0), tds.get(1), tds.get(3));				
						CurrencyRate rate = new CurrencyRate();
						rate.setDate(date);
						rate.setToCurrency(toCurrency);
						rate.setFromCurrency(defaultFromCurrency);
						rate.setValue(Double.parseDouble(tds.get(4).replace(',', '.')));
						
						return rate;
					}
					
					return null;
				})
				.filter((rate) -> rate != null)
				.collect(Collectors.toList());
			
			TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
		        @Override
		        protected void doInTransactionWithoutResult(TransactionStatus status) {
		        	for (CurrencyRate rate : data) {
		        		entityManager.merge(rate);
		        	}
					historyRepository.save(new ParserHistory(date, data.size()));
		        }
		    });
			
	
			logger.info("[{}] Page parse completed, rates found: {}", date, data.size());
			
			return CompletableFuture.completedFuture(data);
		} catch(IOException ex) {
			throw new CompletionException(ex);
		}
	}
}
