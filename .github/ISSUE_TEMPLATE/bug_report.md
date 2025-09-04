---
name: Bug Report
about: Report a bug with full system context
title: '[BUG] '
labels: ['bug', 'needs-investigation']
assignees: ''

---

## Bug Description
**Summary**: Brief description of the bug

**Impact**: How does this bug affect users, system performance, or business operations?

**Severity**: Critical / High / Medium / Low

## System Context
**Environment**: Development / Staging / Production

**Component**: Which service/module is affected?

**Tenant Context**: Does this affect specific stores or all tenants?

**Version**: Application version, commit hash, or tag

## Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3
4. ...

## Expected Behavior
Clear description of what should happen

## Actual Behavior
Clear description of what actually happens

## Error Evidence
### Logs
```
Paste relevant log entries here
```

### Screenshots
If applicable, add screenshots to help explain the problem

### Network/API Requests
```json
// Request
{
  "example": "data"
}

// Response
{
  "error": "example error"
}
```

## System Information
- **OS**: [e.g., macOS 14.2, Ubuntu 22.04]
- **Java Version**: [e.g., OpenJDK 21.0.1]
- **Database**: [e.g., PostgreSQL 15.4]
- **Browser/Client**: [if applicable]

## Architecture Impact
- [ ] **Security**: Does this expose sensitive data or create vulnerabilities?
- [ ] **Data Integrity**: Could this cause data corruption or loss?
- [ ] **Performance**: Does this affect system performance or scalability?
- [ ] **Multi-tenancy**: Does this affect tenant isolation?

## Investigation Notes
**Root Cause Hypothesis**: Initial thoughts on what might be causing this

**Related Components**: What other systems might be involved?

**Recent Changes**: Any recent deployments or configuration changes?

## Additional Context
Add any other context about the problem here, including:
- Frequency of occurrence
- User types affected
- Business impact assessment
- Workarounds if any exist

---

**Priority**: P0 (Critical) / P1 (High) / P2 (Medium) / P3 (Low)
**Assignee**: @mention relevant team member if known
