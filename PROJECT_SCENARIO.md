# Classroom Booking System — Project Scenario

## Business scenario

A facility-management company owns 20 training classrooms of different sizes. The rooms can be rented by external customers, including companies and individual clients. Each classroom has a name, seating capacity, equipment such as projectors or whiteboards, and an accessibility attribute indicating whether it is accessible for people with disabilities.

A company booking user can reserve a room for a customer. The user supplies the date and time range, required capacity, equipment, and accessibility needs. The system checks that the chosen classroom satisfies those requirements and is not already booked during the requested time. When the booking is valid, the system saves it and associates it with both the customer and the booking user who created it.

Bookings may have optional comments for allergies or other special requirements the facility should know about. Facility users need to view upcoming bookings by classroom, by customer, or across all classrooms to manage the schedule.

## Main use cases

### Register a customer

Register an external customer as either a company or an individual client.

### Manage classrooms

Create, view, update, and remove classroom records as needed. Store the classroom name, seating capacity, equipment, and accessibility.

### Search for available classrooms

Enter a date/time range, required capacity, equipment, and accessibility requirement. Show only rooms that meet those criteria and have no overlapping booking.

### Create a booking

Select a customer and booking user, specify the date/time and room requirements, then choose a suitable classroom. Validate the room and time conflict before saving the booking. Optional comments may be included.

### View the schedule

Show upcoming bookings for a selected classroom, a selected customer, or all classrooms, sorted by start time.

## Functional requirements

The console application must allow users to:

1. Register company and individual customers.
2. Manage the 20 classrooms, including name, capacity, equipment, and accessibility.
3. Search for available rooms by date/time, capacity, equipment, and accessibility.
4. Create a booking for a customer and associate the booking user who created it.
5. Add optional comments to a booking.
6. Prevent a classroom from being double-booked during overlapping time ranges.
7. View bookings for a specific classroom.
8. View bookings for a specific customer.
9. View all upcoming bookings.
10. Store and load data using SQL and JDBC.
11. Interact with the application through a console menu.

## Technical and quality requirements

- Model the domain with a UML class diagram and meaningful classes, methods, and relationships.
- Demonstrate encapsulation, abstraction, polymorphism, and appropriate object-oriented design.
- Use collections, streams, and lambda expressions for searching rooms and filtering/sorting bookings.
- Use SQL and JDBC for database creation/management and insert, update, delete, and retrieval operations.
- Keep classes focused, use exceptions appropriately, and avoid unused code.
- Use a Git repository with frequent small commits, clear messages, and at least one feature branch merged into `main`.
- Add JUnit tests for core business logic where appropriate.

## Scope note

This scenario defines room selection, customer/user associations, comments, and booking schedule views. It does not specify additional booking policies; implementation should not assume extra time-window, cancellation, or recurrence rules.
