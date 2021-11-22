package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.protobuf.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerErrorException;
import reactor.core.publisher.Mono;

import java.rmi.ServerError;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Model {

    private final WebClient.Builder webClient;
    private final String reliableTheatreBaseUrl = "https://reliabletheatrecompany.com";
    private final String unreliableTheatreBaseUrl = "https://unreliabletheatrecompany.com";
    private final String key = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    private final String prefix = "https://";
    private ArrayList<Booking> bookings;
    private final String showsRoute = "shows";
    private final String seatsRoute = "seats";
    private final String ticketRoute = "ticket";
    private final String timesRoute = "times";

    @Autowired
    public Model(WebClient.Builder webClientBuilder) {
        bookings = new ArrayList<>();
        webClient = webClientBuilder;
    }

    public List<Show> getShows() {
        List<Show> shows;
        shows = getShows(reliableTheatreBaseUrl);
        shows.addAll(Objects.requireNonNull(getShows(unreliableTheatreBaseUrl)));
        return new ArrayList<>(shows);
    }

    private List<Show> getShows(String url) {
        Collection<Show> shows = new ArrayList<>();
        try{
            shows = Objects.requireNonNull(webClient.baseUrl(url).build().get()
                    .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute).queryParam("key", key).build())
                    .retrieve()
                    .onStatus(HttpStatus::isError, clientResponse -> {
                        System.out.println(url+" responded with "+clientResponse.statusCode());
                        return Mono.error(new HttpServerErrorException(clientResponse.statusCode()));
                    })
                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {
                    })
                    .retry(5)
                    .block())
                    .getContent();
        }catch (Exception e){
            System.out.println(url+" failed to respond after 5 retries. Stacktrace:\n"+Arrays.toString(e.getStackTrace()));
        }

        return new ArrayList<>(shows);
    }


    public Show getShow(String company, UUID showId) {
        try{
            return Objects.requireNonNull(webClient.baseUrl(prefix + company).build().get()
                    .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute).pathSegment(showId.toString()).queryParam("key", key).build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Show>() {
                    })
                    .retry(5)
                    .block());
        }catch (Exception e){
            System.out.println(company+" failed to respond after 5 retries. Stacktrace:\n"+Arrays.toString(e.getStackTrace()));
        }
        return new Show();
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        try{
            var times = Objects.requireNonNull(webClient.baseUrl(prefix + company).build().get()
                    .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                            .pathSegment(showId.toString())
                            .pathSegment(timesRoute)
                            .queryParam("key", key).build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<CollectionModel<LocalDateTime>>() {
                    })
                    .retry(5)
                    .block())
                    .getContent();
            return new ArrayList<>(times);
        }catch (Exception e){
            System.out.println(company+" failed to respond after 5 retries. Stacktrace:\n"+Arrays.toString(e.getStackTrace()));
        }
        return new ArrayList<>();
    }

   /*private <T> Collection<T> getCollectionRequest(java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> function,WebClient wc){
        return wc.get().uri(function)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<T>>() {
                }).block().getContent();

    }*/

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        try{
        var seats = Objects.requireNonNull(webClient.baseUrl(prefix + company).build().get()
                .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                        .pathSegment(showId.toString())
                        .pathSegment(seatsRoute)
                        .queryParam("time", time)
                        .queryParam("available", true)
                        .queryParam("key", key).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {
                })
                .retry(5)
                .block()).getContent();
            return new ArrayList<>(seats);
        }catch (Exception e){
            System.out.println(company+" failed to respond after 5 retries. Stacktrace:\n"+Arrays.toString(e.getStackTrace()));
        }
        return new ArrayList<>();
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        try {
            return Objects.requireNonNull(webClient.baseUrl(prefix + company).build().get()
                    .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                            .pathSegment(showId.toString())
                            .pathSegment(seatsRoute)
                            .pathSegment(seatId.toString())
                            .queryParam("key", key).build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Seat>() {
                    })
                    .retry(5)
                    .block());
        }catch (Exception e){
            System.out.println(company+" failed to respond after 5 retries. Stacktrace:\n"+Arrays.toString(e.getStackTrace()));
        }
        return new Seat();

    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        return Objects.requireNonNull(webClient.baseUrl(prefix + company).build().get()
                .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                        .pathSegment(showId.toString())
                        .pathSegment(seatsRoute)
                        .pathSegment(seatId.toString())
                        .pathSegment(ticketRoute)
                        .queryParam("key", key).build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                })
                .retry(5)
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
        HashMap<String, Integer> counting = new HashMap<>();
        for (Booking b : bookings) {
            if (counting.containsKey(b.getCustomer()))
                counting.put(b.getCustomer(), counting.get(b.getCustomer()) + b.getTickets().size());
            else
                counting.put(b.getCustomer(), b.getTickets().size());
        }
        int maxEntry = 0;
        for (Map.Entry<String, Integer> entry : counting.entrySet()) {
            if (entry.getValue() > maxEntry) {
                maxEntry = entry.getValue();
                bestCustomers.clear();
                bestCustomers.add(entry.getKey());
            }
            if (entry.getValue() == maxEntry) {
                bestCustomers.add(entry.getKey());
            }
        }

        // TODO: return the best customer (highest number of tickets, return all of them if multiple customers have an equal amount)
        return bestCustomers;
    }

    public void confirmQuotes(List<Quote> quotes, String customer) {
        List<Ticket> tickets = new ArrayList<>();
        try {
            quotes.forEach(x -> {
                Ticket t = webClient.baseUrl(prefix + x.getCompany())
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
                            //log status code when error response code is received
                            System.out.println(response.statusCode().toString());
                            return Mono.error(new ServiceException(response.toString()));
                        })
                        .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                        })
                        .retry(5)
                        .block();
                tickets.add(t);
                System.out.println(t.getCustomer());
            });
        }catch(Exception e){
            //all-or-nothing implementation: delete all tickets that were already booked if something goes wrong
            System.out.println("Failed to book ticket, deleting already booked tickets");
            deleteTickets(tickets);
            tickets.clear();
        }


        Booking booking = new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, customer);
        bookings.add(booking);

    }

    private void deleteTickets(List<Ticket> tickets) {
        try {
            tickets.forEach(x -> {
                webClient.baseUrl(prefix + x.getCompany())
                        .build()
                        .delete()
                        .uri(uriBuilder -> uriBuilder.pathSegment(showsRoute)
                                .pathSegment(x.getShowId().toString())
                                .pathSegment(seatsRoute)
                                .pathSegment(x.getSeatId().toString())
                                .pathSegment(ticketRoute)
                                .pathSegment(x.getTicketId().toString())
                                .queryParam("key", key).build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Ticket>() {
                        })
                        .retry(10)
                        .block();
            });
        }catch (Exception e){
            System.out.println(" failed to delete ticket after 10 retries. Stacktrace:\n"+Arrays.toString(e.getStackTrace()));
        }
    }
}
