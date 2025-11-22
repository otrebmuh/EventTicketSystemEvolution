"""
Load Testing for Event Ticket Booking System

Tests Requirements: 10.1-10.5 (Performance requirements)

This load test simulates realistic user behavior patterns:
- User registration and login
- Event browsing and searching
- Ticket selection and purchase
- Concurrent operations to test system under load
"""

from locust import HttpUser, task, between, events
from faker import Faker
import random
import json
import time

fake = Faker()

class EventTicketUser(HttpUser):
    """
    Simulates a user interacting with the Event Ticket Booking System.
    
    Tests system behavior under load including:
    - Concurrent user registrations
    - Simultaneous event searches
    - Concurrent ticket purchases
    - Database connection pool stress
    """
    
    wait_time = between(1, 3)  # Wait 1-3 seconds between tasks
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.auth_token = None
        self.user_email = None
        self.event_id = None
        self.ticket_type_id = None
    
    def on_start(self):
        """Called when a simulated user starts"""
        self.register_and_login()
    
    def register_and_login(self):
        """Register a new user and login"""
        # Generate unique user data
        self.user_email = f"loadtest.{fake.uuid4()}@example.com"
        password = "LoadTest123!@#"
        
        # Register
        registration_data = {
            "firstName": fake.first_name(),
            "lastName": fake.last_name(),
            "email": self.user_email,
            "dateOfBirth": "1990-01-01",
            "password": password
        }
        
        with self.client.post(
            "/api/auth/register",
            json=registration_data,
            catch_response=True,
            name="Register User"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Registration failed: {response.status_code}")
        
        # Small delay to simulate email verification
        time.sleep(0.5)
        
        # Login
        login_data = {
            "email": self.user_email,
            "password": password,
            "rememberMe": False
        }
        
        with self.client.post(
            "/api/auth/login",
            json=login_data,
            catch_response=True,
            name="Login User"
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    self.auth_token = data.get("token")
                    if self.auth_token:
                        response.success()
                    else:
                        response.failure("No token in response")
                except Exception as e:
                    response.failure(f"Login response parsing failed: {str(e)}")
            else:
                response.failure(f"Login failed: {response.status_code}")
    
    def get_auth_headers(self):
        """Get authorization headers"""
        if self.auth_token:
            return {"Authorization": f"Bearer {self.auth_token}"}
        return {}
    
    @task(3)
    def browse_events(self):
        """Browse available events - most common user action"""
        with self.client.get(
            "/api/events",
            headers=self.get_auth_headers(),
            catch_response=True,
            name="Browse Events"
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    # Store an event ID for later use
                    if isinstance(data, list) and len(data) > 0:
                        self.event_id = data[0].get("id")
                    elif isinstance(data, dict) and "content" in data:
                        if len(data["content"]) > 0:
                            self.event_id = data["content"][0].get("id")
                    response.success()
                except Exception as e:
                    response.failure(f"Failed to parse events: {str(e)}")
            else:
                response.failure(f"Browse events failed: {response.status_code}")
    
    @task(2)
    def search_events(self):
        """Search for events by keyword"""
        search_terms = ["concert", "festival", "conference", "sports", "theater"]
        search_query = random.choice(search_terms)
        
        with self.client.get(
            f"/api/events/search?q={search_query}",
            headers=self.get_auth_headers(),
            catch_response=True,
            name="Search Events"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Search failed: {response.status_code}")
    
    @task(1)
    def view_event_details(self):
        """View details of a specific event"""
        if not self.event_id:
            return
        
        with self.client.get(
            f"/api/events/{self.event_id}",
            headers=self.get_auth_headers(),
            catch_response=True,
            name="View Event Details"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"View event failed: {response.status_code}")
    
    @task(1)
    def check_ticket_availability(self):
        """Check ticket availability for an event"""
        if not self.event_id:
            return
        
        with self.client.get(
            f"/api/tickets/availability/{self.event_id}",
            headers=self.get_auth_headers(),
            catch_response=True,
            name="Check Ticket Availability"
        ) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    # Store ticket type ID for purchase
                    if isinstance(data, list) and len(data) > 0:
                        self.ticket_type_id = data[0].get("id")
                    response.success()
                except Exception as e:
                    response.failure(f"Failed to parse availability: {str(e)}")
            else:
                response.failure(f"Check availability failed: {response.status_code}")
    
    @task(1)
    def reserve_tickets(self):
        """Reserve tickets - tests inventory management under load"""
        if not self.ticket_type_id:
            return
        
        reservation_data = {
            "ticketTypeId": self.ticket_type_id,
            "quantity": random.randint(1, 4)
        }
        
        with self.client.post(
            "/api/tickets/reserve",
            json=reservation_data,
            headers=self.get_auth_headers(),
            catch_response=True,
            name="Reserve Tickets"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            elif response.status_code == 409:
                # Conflict - tickets not available (expected under high load)
                response.success()
            else:
                response.failure(f"Reserve failed: {response.status_code}")
    
    @task(1)
    def view_profile(self):
        """View user profile"""
        with self.client.get(
            "/api/auth/profile",
            headers=self.get_auth_headers(),
            catch_response=True,
            name="View Profile"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"View profile failed: {response.status_code}")


class EventOrganizerUser(HttpUser):
    """
    Simulates an event organizer creating and managing events.
    
    Tests organizer-specific operations under load.
    """
    
    wait_time = between(2, 5)
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.auth_token = None
        self.event_id = None
    
    def on_start(self):
        """Register and login as organizer"""
        email = f"organizer.{fake.uuid4()}@example.com"
        password = "Organizer123!@#"
        
        # Register
        self.client.post("/api/auth/register", json={
            "firstName": fake.first_name(),
            "lastName": fake.last_name(),
            "email": email,
            "dateOfBirth": "1985-01-01",
            "password": password
        })
        
        time.sleep(0.5)
        
        # Login
        response = self.client.post("/api/auth/login", json={
            "email": email,
            "password": password
        })
        
        if response.status_code == 200:
            self.auth_token = response.json().get("token")
    
    def get_auth_headers(self):
        if self.auth_token:
            return {"Authorization": f"Bearer {self.auth_token}"}
        return {}
    
    @task(2)
    def create_event(self):
        """Create a new event"""
        event_data = {
            "name": f"{fake.catch_phrase()} {fake.uuid4()[:8]}",
            "description": fake.text(max_nb_chars=200),
            "eventDate": "2025-12-31T19:00:00Z",
            "venueName": fake.company(),
            "venueAddress": fake.address(),
            "category": random.choice(["CONCERT", "CONFERENCE", "SPORTS", "THEATER"]),
            "status": "PUBLISHED"
        }
        
        with self.client.post(
            "/api/events",
            json=event_data,
            headers=self.get_auth_headers(),
            catch_response=True,
            name="Create Event"
        ) as response:
            if response.status_code in [200, 201]:
                try:
                    self.event_id = response.json().get("id")
                    response.success()
                except:
                    response.failure("Failed to parse event ID")
            else:
                response.failure(f"Create event failed: {response.status_code}")
    
    @task(1)
    def create_ticket_type(self):
        """Create ticket types for event"""
        if not self.event_id:
            return
        
        ticket_data = {
            "eventId": self.event_id,
            "name": random.choice(["VIP", "General Admission", "Early Bird"]),
            "description": fake.sentence(),
            "price": round(random.uniform(25.0, 200.0), 2),
            "quantityAvailable": random.randint(50, 500),
            "saleStartDate": "2024-01-01T00:00:00Z",
            "saleEndDate": "2025-12-30T23:59:59Z"
        }
        
        with self.client.post(
            "/api/tickets/types",
            json=ticket_data,
            headers=self.get_auth_headers(),
            catch_response=True,
            name="Create Ticket Type"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Create ticket type failed: {response.status_code}")


@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Called when load test starts"""
    print("=" * 60)
    print("Starting Event Ticket Booking System Load Test")
    print("=" * 60)
    print(f"Target: {environment.host}")
    print(f"Users: {environment.runner.target_user_count if hasattr(environment.runner, 'target_user_count') else 'N/A'}")
    print("=" * 60)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Called when load test stops"""
    print("=" * 60)
    print("Load Test Completed")
    print("=" * 60)
    
    stats = environment.stats
    print(f"Total Requests: {stats.total.num_requests}")
    print(f"Total Failures: {stats.total.num_failures}")
    print(f"Average Response Time: {stats.total.avg_response_time:.2f}ms")
    print(f"Requests/sec: {stats.total.total_rps:.2f}")
    print("=" * 60)
