# Stakeholder-oriented source organization

This version physically moves the existing stakeholder-owned controllers, DAO classes, and React pages into stakeholder folders. It updates Java package declarations/imports and React imports/routes for those moves.

## Shared code deliberately remains shared
- Backend: `Main`, authentication, dashboard, lookup, notification, applicant portal, common DAO classes, models, config, utilities, and shared `AuthService`.
- Frontend: authentication, layout, dashboard, notifications, applicant pages, common UI, API clients, and navigation config.
- Attendance and leave APIs are in Operations because Operations owns the workflows, but they enforce role-specific access for employees and other staff.
- `EmployeeController` is under Employee because it also provides employee self-service profile endpoints; role checks continue to govern directory operations.

## Presentation safety
The original archive's `.git`, generated build output, dependency directories, and local `backend/config.properties` were excluded. Recreate `backend/config.properties` from `backend/config.properties.example` with your own SQL Server credentials.
