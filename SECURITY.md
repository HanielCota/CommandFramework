# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| main    | ✅        |
| < v0.1  | ❌        |

## Reporting a Vulnerability

If you discover a security vulnerability in this project, please report it responsibly:

1. **Do NOT** open a public issue
2. Email: [security@hanielcota.dev](mailto:security@hanielcota.dev) (or open a private security advisory on GitHub)
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Affected versions
   - Potential impact

We will respond within **72 hours** and work on a fix before public disclosure.

## Security Considerations

- This framework processes player input. Always validate and sanitize command arguments.
- Avoid logging sensitive player data (UUIDs, IPs, messages).
- When using `@Async`, ensure thread-safety with platform APIs (especially Paper/Bukkit).
