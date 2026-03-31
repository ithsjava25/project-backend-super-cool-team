## Entitys

- **Staff**: Base employee with personal info
- **HR/Management/Consultant**: Role profiles 1:1 to Staff
- **ReportForm**: Case/problem form to be reported by staff
- **EmploymentForm**: Employee-process (HR-only)
- **Attachment**: Filemetadata + S3-key for uploaded files

## Flow 1: ReportForm

`SUBMITTED → IN_PROGRESS → RESOLVED → CLOSED`

- Staff creates → HR assigns → case assignee solves it → HR/Manager closes

## Flow 2: EmploymentForm

`DRAFT → SUBMITTED → APPROVED/REJECTED → COMPLETED`

- HR fills out the form → HR submits → Manager approves → System creates a Staff with the submitted info automatically
- Only HR can create an EmploymentForm

## Authorization

- **Consultant**: Read/update their own ReportForm, upload files
- **HR**: Read/handle all ReportForms, create EmploymentForm (HR-only!)
- **Management**: Read department-errands, approve status & EmploymentForm

## MVP Order

1. Enums (Status, Priority, IssueType, Department)
2. ReportForm CRUD + authorization
3. EmploymentForm + auto-create Staff
4. S3-file + Download

