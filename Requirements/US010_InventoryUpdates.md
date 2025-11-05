# User Story: Inventory Updates (US010)

**As an** event organizer  
**I want to** update ticket inventory  
**So that** I can manage ticket availability

## Acceptance Criteria

1. Event organizer can:
   - Add more tickets to existing inventory
   - Remove tickets from inventory
   - Transfer tickets between types
2. System updates inventory when:
   - Tickets are reserved
   - Tickets are purchased
   - Reservations expire
   - Tickets are cancelled
3. System prevents inventory updates that would result in negative quantities
4. System maintains inventory history
5. System provides real-time inventory status 