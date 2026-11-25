# Classroom Booking System — Initial UML Design

This is a first-pass domain and component model to guide implementation. Users book a classroom for a date and time; a booking is confirmed immediately if it does not overlap another active booking for the same room. There is no approval workflow.

## Initial scope

Keep the first version focused on users, classrooms, one-time bookings, and recurring bookings. Equipment requests and setup/cleanup buffers are out of scope. Start recurring bookings with a simple weekly schedule over a defined date range; defer advanced recurrence rules and per-occurrence edits.

## System components

```mermaid
flowchart LR
    Client[Web or mobile client]
    API[Booking API]
    BookingService[Booking application service]
    RoomService[Room and availability service]
    Identity[Identity and access component]
    Repositories[Persistence repositories]
    Database[(Relational database)]
    Notifications[Notification component]

    Client --> API
    API --> Identity
    API --> BookingService
    BookingService --> RoomService
    BookingService --> Repositories
    RoomService --> Repositories
    Repositories --> Database
    BookingService -. booking updates .-> Notifications
```

| Component | Responsibility |
| --- | --- |
| Web or mobile client | Lets users find rooms, view availability, and create or manage bookings. |
| Booking API | Validates requests at the system boundary and exposes booking and room operations. |
| Identity and access | Identifies users and applies role-based permissions (for example, requester and administrator). |
| Booking application service | Coordinates booking creation, cancellation, and status changes. |
| Room and availability service | Finds rooms and checks whether a requested time interval is available. |
| Persistence repositories | Load and store users, rooms, and bookings without exposing database details to the domain. |
| Notification component | Sends booking confirmations or status updates; it can be integrated after the core booking flow. |

## Core class diagram

```mermaid
classDiagram
    class User {
        +UUID id
        +String name
        +String email
        +UserRole role
        +isActive() boolean
    }

    class UserRole {
        <<enumeration>>
        REQUESTER
        ADMINISTRATOR
    }

    class Classroom {
        +UUID id
        +String name
        +String building
        +int capacity
        +String location
        +boolean active
        +canAccommodate(int attendees) boolean
    }

    class Booking {
        +UUID id
        +UUID seriesId
        +UUID requesterId
        +UUID classroomId
        +LocalDateTime startsAt
        +LocalDateTime endsAt
        +int attendees
        +BookingStatus status
        +String purpose
        +cancel()
        +overlaps(LocalDateTime start, LocalDateTime end) boolean
    }

    class BookingSeries {
        +UUID id
        +UUID requesterId
        +UUID classroomId
        +LocalDate startsOn
        +LocalDate endsOn
        +LocalTime startsAt
        +LocalTime endsAt
        +int interval
        +Set~DayOfWeek~ daysOfWeek
        +SeriesStatus status
        +generateOccurrences() List~Booking~
        +cancel()
    }

    class SeriesStatus {
        <<enumeration>>
        ACTIVE
        CANCELLED
    }

    class BookingStatus {
        <<enumeration>>
        CONFIRMED
        CANCELLED
    }

    class BookingService {
        +createBooking(CreateBookingCommand) Booking
        +cancelBooking(UUID bookingId, UUID actorId) void
        +getBookingsForUser(UUID userId) List~Booking~
    }

    class AvailabilityService {
        +isAvailable(UUID classroomId, LocalDateTime start, LocalDateTime end) boolean
        +findAvailableClassrooms(LocalDateTime start, LocalDateTime end, int attendees) List~Classroom~
    }

    class UserRepository {
        <<interface>>
        +findById(UUID id) Optional~User~
    }

    class ClassroomRepository {
        <<interface>>
        +findById(UUID id) Optional~Classroom~
        +findActive() List~Classroom~
    }

    class BookingRepository {
        <<interface>>
        +save(Booking) Booking
        +findById(UUID id) Optional~Booking~
        +existsOverlappingBooking(UUID classroomId, LocalDateTime start, LocalDateTime end) boolean
        +existsOverlappingBookingForRequester(UUID requesterId, LocalDateTime start, LocalDateTime end) boolean
        +findByRequesterId(UUID requesterId) List~Booking~
    }

    User --> UserRole
    Booking --> BookingStatus
    BookingSeries --> SeriesStatus
    User "1" --> "0..*" Booking : requests
    Classroom "1" --> "0..*" Booking : reserved by
    User "1" --> "0..*" BookingSeries : creates
    Classroom "1" --> "0..*" BookingSeries : scheduled for
    BookingSeries "1" *--> "1..*" Booking : generates occurrences
    BookingService --> UserRepository
    BookingService --> ClassroomRepository
    BookingService --> BookingRepository
    BookingService --> AvailabilityService
    AvailabilityService --> ClassroomRepository
    AvailabilityService --> BookingRepository
```

## Booking creation flow

```mermaid
sequenceDiagram
    actor Requester
    participant API as Booking API
    participant Service as BookingService
    participant Availability as AvailabilityService
    participant Rooms as ClassroomRepository
    participant Bookings as BookingRepository

    Requester->>API: Request room and time slot
    API->>Service: createBooking(command, requesterId)
    Service->>Rooms: Find active classroom
    Rooms-->>Service: Classroom
    Service->>Service: Validate time range and capacity
    Service->>Availability: Check classroom availability
    Availability->>Bookings: Check overlapping active booking
    Bookings-->>Availability: Available / conflict
    Availability-->>Service: Availability result
    alt Valid and available
        Service->>Bookings: Save confirmed booking
        Bookings-->>Service: Saved booking
        Service-->>API: Booking
        API-->>Requester: Booking confirmation
    else Invalid, unavailable, or unauthorized
        Service-->>API: Domain error
        API-->>Requester: Explain why request failed
    end
```

## Initial business rules

1. A booking is confirmed immediately when created successfully; administrator approval is not required.
2. A booking can be made no more than 7 days before it starts.
3. A booking must have a start time before its end time.
4. The classroom must be active and have capacity for the requested attendee count.
5. A room cannot have overlapping bookings that are not cancelled. Adjacent intervals are allowed (for example, 10:00–11:00 and 11:00–12:00).
6. A requester cannot have overlapping active bookings, even when the bookings are for different rooms. Adjacent bookings are allowed.
7. A recurring booking creates individual booking occurrences according to its recurrence pattern. Each occurrence becomes a confirmed booking when it enters the 7-day booking window and passes room availability, capacity, and requester conflict checks.
8. Cancellation is an optional action: a requester may cancel their own booking, and administrators may manage any booking. No 24-hour cancellation cutoff is specified.
9. Room availability, requester availability, and booking creation must be checked transactionally, or protected by a database constraint/locking strategy, to avoid conflicting simultaneous requests.

## Open decisions

- Should recurring bookings later support additional patterns (for example, monthly schedules), excluded dates, or editing a single occurrence?
- Which identity provider and user roles should be supported?
- What are the timezone and opening-hours policies?
- Should notifications be email, in-app, or both?
