# Project Requirements and Submission Checklist

See the separate [project scenario](PROJECT_SCENARIO.md) for the domain narrative and user workflows.

## Project scenario

A facility-management company owns 20 training classrooms of different sizes. External customers—companies and individuals—rent the rooms. Each classroom has a name, seating capacity, equipment, and an accessibility attribute. A company booking user selects a date/time range, required capacity, equipment, and accessibility needs. The application validates the selected classroom's fit and availability, then saves the booking linked to its customer and booking user. Optional booking comments may record allergies or other special requirements.

The application is a console program backed by a relational SQL database accessed through JDBC. It must support viewing upcoming bookings by classroom, by customer, and across the system.

## Functional requirements

The application must allow users to:

- Register company and individual customers.
- Manage the 20 classrooms, including name, capacity, equipment, and accessibility.
- Search for available classrooms by date/time, capacity, equipment, and accessibility.
- Create a booking for a customer and associate the booking user who created it.
- Add optional comments to a booking.
- Prevent double-booking a classroom during overlapping time ranges.
- View bookings for a selected classroom or customer, and view all upcoming bookings.
- Use a console menu and persist/load data using SQL and JDBC.

## Domain modeling and OOP

- Create a UML class diagram that reflects the scenario and relationships.
- Use meaningful class, method, and variable names; keep classes focused.
- Demonstrate encapsulation, abstraction, polymorphism, and correctly modeled relationships.
- Add comments that explain class purpose and important logic.

## Collections and functional programming

Use Java collections, streams, and lambda expressions where appropriate for:

- Filtering available classrooms by requirements and bookings.
- Filtering and sorting upcoming bookings.

## SQL and JDBC

Use SQL and JDBC to:

- Create and manage the relational database.
- Insert, update, delete, and retrieve data.
- Connect the Java application to the database and close JDBC resources correctly.

## Code quality

- Keep code clean, readable, and focused on the requirements.
- Use exceptions for invalid input and database failures.
- Avoid unused methods and unnecessary features.

## Git workflow requirements

The repository should demonstrate proper Git use:

- Make frequent, small commits.
- Write clear commit messages describing what changed and why.
- Create at least one separate feature branch.
- Merge the completed feature branch back into `main`.

## Testing recommendations

- Add JUnit tests for core domain/business logic.
- Prefer tests of business rules over tests of console input/output.
- Include database integration checks for SQL/JDBC behavior.

## Submission checklist

Before project evaluation, confirm the repository contains:

- [ ] Working core functional requirements.
- [ ] Correct OOP modeling, including encapsulation, interfaces/inheritance, and polymorphism.
- [ ] SQL/JDBC persistence that saves and loads data.
- [ ] Collections and streams for searching, filtering, or sorting.
- [ ] Exception-based error handling for invalid input and database failures.
- [ ] A README with setup instructions and a UML class diagram.
- [ ] Evidence of small, clear commits and a feature branch merged into `main`.

**Design before implementation.** Review the [UML design](docs/uml-design.md) and [feature-by-feature implementation plan](docs/implementation-plan.md) as the project progresses.
