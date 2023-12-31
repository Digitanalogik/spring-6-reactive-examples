package fi.soininen.tatu.spring6reactive.repositories;

import fi.soininen.tatu.spring6reactive.domain.Person;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonRepositoryImplTest {

    PersonRepository personRepository = new PersonRepositoryImpl();

    // This is example of bad way to work with reactive streams
    // Person object is being fetched, but the process is blocked meanwhile
    // @Disabled
    @Test
    void testMonoByIdBlock() {
        Mono<Person> personMono = personRepository.getById(1);
        Person person = personMono.block();
        System.out.println(person.toString());
    }

    // This is preferred way to work with reactive streams
    // Subscriber proceeds asynchronously when the data is available (=published)
    @Test
    void testGetByIdSubscriber() {
        Mono<Person> personMono = personRepository.getById(1);
        personMono.subscribe(person -> {
            System.out.println(person.toString());
        });
    }

    // Map Mono object using lambda function syntax
    // Note: this is exactly the same as testMapOperationMethodReference() method but in longer format
    @Test
    void testMapOperationLambdaSyntax() {
        Mono<Person> personMono = personRepository.getById(1);

        personMono.map(person -> {
            return person.getFirstName();
        }).subscribe(firstName -> {
            System.out.println(firstName);
        });
    }


    // Map Mono object using method reference
    // Note: this is exactly the same as testMapOperationLambdaSyntax() method but using syntactic sugar :)
    @Test
     void testMapOperationMethodReference() {
        Mono<Person> personMono = personRepository.getById(1);

        personMono.map(Person::getFirstName)
            .subscribe(System.out::println);
    }

    @Test
    void testFluxBlockFirst() {
        Flux<Person> personFlux = personRepository.findAll();

        Person person = personFlux.blockFirst();

        System.out.println(person.toString());
    }

    @Test
    void testFluxSubscriber() {
        Flux<Person> personFlux = personRepository.findAll();

        personFlux.subscribe(person -> {
            System.out.println(person.toString());
        });
    }

    @Test
    void testFluxMap() {
        Flux<Person> personFlux = personRepository.findAll();

        personFlux.map(Person::getFirstName)
            .subscribe(firstName -> System.out.println(firstName));
    }

    @Test
    void testFluxToList() {
        Flux<Person> personFlux = personRepository.findAll();

        Mono<List<Person>> listMono = personFlux.collectList();

        listMono.subscribe(list -> {
           list.forEach(person -> System.out.println(person.getFirstName()));
        });
    }

    @Test
    void testFilterOnName() {
        personRepository.findAll()
            .filter(person -> person.getFirstName().equals("Mario"))
            .subscribe(person -> System.out.println(person.getFirstName()));
    }

    @Test
    void testGetById() {
        Mono<Person> marioMono = personRepository.findAll()
            .filter(person -> person.getFirstName().equals("Mario"))
            .next();

        // next()
        // Emit only the first item emitted by this Flux, into a new Mono.
        // If called on an empty Flux, emits an empty Mono.

        marioMono.subscribe(person -> System.out.println(person.getFirstName()));
    }

    @Test
    void testFindPersonByIdNotFound() {
        Flux<Person> personFlux = personRepository.findAll();

        // Important: always use final values with reactive streams!
        // Variables are not allowed to mutate while processing the stream
        final Integer id = 8;

        // single()
        // Expect and emit a single item from this Flux source or signal
        // - NoSuchElementException for an empty source, or
        // - IndexOutOfBoundsException for a source with more than one element.
        Mono<Person> personMono = personFlux
            .filter(person -> person.getId() == id)
            .single().doOnError(throwable -> {
                System.out.println("Error occurred in the Flux");
                System.out.println(throwable.toString());
        });

        // Without subscriber there is no back pressure, thus errors won't be raised even with empty stream
        // But now with the subscribe method, the process is trying to read the stream but fails because it is empty
        personMono.subscribe(person -> {
            System.out.println(person.toString());
        }, throwable -> {
            System.out.println("Error occurred in the Mono");
            System.out.println(throwable.toString());
        });
    }

    @Test
    void testGetIdFound() {
        Mono<Person> personMono = personRepository.getById(3);

        assertTrue(personMono.hasElement().block());
    }

    @Test
    void testGetIdFoundStepVerifier() {
        Mono<Person> personMono = personRepository.getById(3);

        StepVerifier.create(personMono)
            .expectNextCount(1)
            .verifyComplete();

        personMono.subscribe(person -> {
            System.out.println(person.getFirstName());
        });
    }


    @Test
    void testGetIdNotFound() {
        Mono<Person> personMono = personRepository.getById(5);

        assertFalse(personMono.hasElement().block());
    }

    @Test
    void testGetIdNotFoundStepVerifier() {
        Mono<Person> personMono = personRepository.getById(5);

        StepVerifier.create(personMono)
            .expectNextCount(0)
            .verifyComplete();

        personMono.subscribe(person -> {
            System.out.println(person.getFirstName());
        });
    }

}