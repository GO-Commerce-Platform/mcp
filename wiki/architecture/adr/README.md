# Architecture Decision Records

## Overview

This directory contains the Architecture Decision Records (ADRs) for the GO-Commerce MCP server. Each ADR documents a significant architectural decision, including the context, the decision made, and the consequences of that decision.

## Index

### Current ADRs

| Number | Title | Status | Created | Description |
|--------|-------|--------|---------|-------------|
| [ADR-0001](0001-multi-tenant-pattern.md) | Multi-tenant Pattern Selection | Accepted | 2025-09-02 | Selection of schema-per-tenant pattern for multi-tenancy |
| [ADR-0002](0002-technology-stack.md) | Technology Stack Selection | Accepted | 2025-09-02 | Core technology choices including Quarkus, Java 21, and supporting technologies |
| [ADR-0003](0003-security-architecture.md) | Security Architecture | Accepted | 2025-09-02 | Comprehensive security architecture including authentication, authorization, and data protection |

### Template

- [ADR Template](0000-template.md) - Template for creating new ADRs

## Creating New ADRs

1. Copy the template file: `0000-template.md`
2. Create a new file with the next number in sequence
3. Fill in all sections of the template
4. Update this index with the new ADR
5. Submit for review

## ADR Process

1. **Proposal**
   - Create new ADR from template
   - Fill in all sections
   - Mark as "Proposed"

2. **Review**
   - Technical review by team
   - Update based on feedback
   - Address concerns

3. **Decision**
   - Mark as "Accepted" or "Rejected"
   - Document final state
   - Update related ADRs

4. **Implementation**
   - Reference ADR in implementation
   - Update if needed during implementation
   - Document any deviations

## Status Definitions

- **Proposed**: Initial proposal, under discussion
- **Accepted**: Approved and being implemented
- **Rejected**: Decided against implementation
- **Deprecated**: No longer relevant but kept for history
- **Superseded**: Replaced by a newer decision

// Copilot: This file may have been generated or refactored by GitHub Copilot.
