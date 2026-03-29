## Entitys

- **Staff**: Baseemployee with personal info
- **HR/Management/Consultant**: Roleprofiles 1:1 to Staff
- **ReportForm**: Case/problem form to be reported by staff
- **EmploymentForm**: Employee-process (HR-only)
- **Attachment**: Filemetadata + S3-key for uploaded files
- **AuditEvent**: Logg for all important events

## Flow 1: ReportForm

`SUBMITTED → IN_PROGRESS → RESOLVED → CLOSED`

- Staff creates → HR assignes → Handläggare solves → HR/Manager closes
- All actions logged in AuditEvent

## Flow 2: EmploymentForm

`DRAFT → SUBMITTED → APPROVED → COMPLETED`

- HR fills out the form → HR submits → Manager approves → System creates a Staff with the submitted info automatiskt
- Only HR can create EmploymentForm

## Authorization

- **Consultant**: Read/update their own ReportForm, upload files
- **HR**: Read/handle all ReportForms, create EmploymentForm (HR-only!)
- **Management**: Read departmenterrands, approve status & EmploymentForm

## MVP Order

1. Enums (Status, Priority, IssueType, Department)
2. ReportForm CRUD + behörigheter
3. EmploymentForm + auto-create Staff
4. S3-filhantering + nedladdning
5. Realtimesuppdates


