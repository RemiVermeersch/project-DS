package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.Date;
import com.google.protobuf.ServiceException;
import org.bouncycastle.util.StringList;
import org.eclipse.jetty.util.DateCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.print.Book;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Model {

    private final WebClient.Builder webClient;
    private final String baseUrl = "https://reliabletheatrecompany.com";
    private final String key = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    private final String prefix = "https://";
    private ArrayList<Booking> bookings;
    private final String showsRoute = "shows";
    private final String seatsRoute = "seats";
    private final String ticketRoute = "ticket";
    private final String timesRoute = "times";
    @Autowired
    public Model(WebClient.Builder webClientBuilder ) {
        bookings = new ArrayList<>();
        webClient = webClientBuilder;
    }

    public List<Show> getShows() {
        // TODO: return all shows
        return new ArrayList<>();
    }

    public Show getShow(String company, UUID showId) {
        return Objects.requireNonNull(webClient.baseUrl(prefix+company).build().get()
                .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute).pathSegment(showId.toString()).queryParam("key", key).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Show>() {
                })
                .block());
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        //Collection<LocalDateTime> times = getCollectionRequest(Arrays.asList("shows",showId.toString(),"times"), webClient.baseUrl(baseUrl).build());
        var times = Objects.requireNonNull(webClient.baseUrl(prefix+company).build().get()
                .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                        .pathSegment(showId.toString())
                        .pathSegment(timesRoute)
                        .queryParam("key", key).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<LocalDateTime>>() {
                })
                .block()).getContent();
        //var test = new ArrayList<LocalDateTime>(times);
        return new ArrayList<>(times);
    }

    private <T> Collection<T> getCollectionRequest(List<String> pathSegments, WebClient wc){
        return wc.get().uri(uriBuilder -> uriBuilder
                                        .path(pathSegments.stream().reduce("",(part,elem) -> part+"/"+elem))
                                        .queryParam("key",key).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<T>>() {
                }).block().getContent();

    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        var seats = Objects.requireNonNull(webClient.baseUrl(prefix+company).build().get()
                .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                        .pathSegment(showId.toString())
                        .pathSegment(seatsRoute)
                        .queryParam("time",time)
                        .queryParam("available", true)
                        .queryParam("key", key).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {
                })
                .block()).getContent();
        return new ArrayList<>(seats);
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        return Objects.requireNonNull(webClient.baseUrl(prefix+company).build().get()
                .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                        .pathSegment(showId.toString())
                        .pathSegment(seatsRoute)
                        .pathSegment(seatId.toString())
                        .queryParam("key", key).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {
                })
                .block());
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        return Objects.requireNonNull(webClient.baseUrl(prefix+company).build().get()
                .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                        .pathSegment(showId.toString())
                        .pathSegment(seatsRoute)
                        .pathSegment(seatId.toString())
                        .pathSegment(ticketRoute)
                        .queryParam("key", key).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                })
                .block());
    }

    public List<Booking> getBookings(String customer) {
        return bookings.stream().filter(x -> x.getCustomer().equals(customer)).collect(Collectors.toList());
    }

    public List<Booking> getAllBookings() {
        return bookings;
    }

    public Set<String> getBestCustomers() {
        Set<String> bestCustomers = new HashSet<>();
        HashMap<String, Integer> counting= new HashMap<>();
        for (Booking b: bookings) {
            if(counting.containsKey(b.getCustomer()))
                counting.put(b.getCustomer(),counting.get(b.getCustomer())+ b.getTickets().size());
            else
                counting.put(b.getCustomer(),b.getTickets().size());
        }
        int maxEntry = 0;
        for (Map.Entry<String,Integer> entry : counting.entrySet())
        {
            if (entry.getValue() > maxEntry)
            {
                maxEntry = entry.getValue();
                bestCustomers.clear();
                bestCustomers.add(entry.getKey());
            }
            if(entry.getValue() == maxEntry){
                bestCustomers.add(entry.getKey());
            }
        }

        // TODO: return the best customer (highest number of tickets, return all of them if multiple customers have an equal amount)
        return bestCustomers;
    }

    public void confirmQuotes(List<Quote> quotes, String customer) {
        List<Ticket> tickets = new ArrayList<>();
        quotes.forEach(x -> { Ticket t = webClient.baseUrl(prefix + x.getCompany())
                                     .build()
                                     .put()
                                     .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                                                                .pathSegment(x.getShowId().toString())
                                                                .pathSegment(seatsRoute)
                                                                .pathSegment(x.getSeatId().toString())
                                                                .pathSegment(ticketRoute)
                                                                .queryParam("customer", customer)
                                                                .queryParam("key", key).build())
                                     .retrieve()
                                     .onStatus(HttpStatus::isError, response -> {
                                        System.out.println(response.statusCode().toString());
                                        System.out.println(response.toString());
                                        return Mono.error(new ServiceException(response.toString()));
                                     })
                                     .bodyToMono(new ParameterizedTypeReference<Ticket>(){})
                                     .block();
                                tickets.add(t);
                                System.out.println(t.getCustomer());
        });
        Booking booking = new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, customer);
        bookings.add(booking);

    }
}
