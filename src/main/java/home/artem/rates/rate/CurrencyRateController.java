package home.artem.rates.rate;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import home.artem.rates.parser.ParserHistoryRepository;
import home.artem.rates.parser.RatesParserService;

@RestController
@RequestMapping("/api/rates")
public class CurrencyRateController {
	Logger logger = LoggerFactory.getLogger(CurrencyRateController.class);
	
	@Autowired
	RatesParserService parserService;
	
	@Autowired
	CurrencyRateRepository rateRepository;
	
	@Autowired
	ParserHistoryRepository historyRepository;
	
	@CrossOrigin(origins = "http://localhost:4200")
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<CurrencyRate>> getRates(
    		@RequestParam(name = "dateFrom", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
    		@RequestParam(name = "dateTo", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo,
    		@RequestParam(name = "toCurrencyCodes", required = true) List<String> toCurrencyCodes) {
    	LocalDate currentDate = LocalDate.now();
    	if (dateFrom.isAfter(currentDate) || dateTo.isAfter(currentDate)) {
    		return new ResponseEntity<List<CurrencyRate>>(HttpStatus.BAD_REQUEST);
    	}
    	
    	if (dateFrom.isAfter(dateTo)) {
    		return new ResponseEntity<List<CurrencyRate>>(HttpStatus.BAD_REQUEST);
    	}
    	
    	parserService.fillMissingDates(dateFrom, dateTo);
   	
		return ResponseEntity.ok(
				rateRepository.findByDateBetweenAndToCurrencyLetterCodeIn(dateFrom, dateTo, toCurrencyCodes));
    }
}
