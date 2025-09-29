# Changelog

## [1.0.1] - 2024-09-30

### Fixed
- Updated GitHub Actions workflow to use `actions/upload-artifact@v4` (from deprecated v3)
- Updated GitHub Actions workflow to use `actions/download-artifact@v4` (from deprecated v3)
- Fixed YAML parsing error by quoting private key environment variable in CI/CD pipeline
- Fixed dependency caching issue by generating `package-lock.json` for contracts
- Changed from `npm install` to `npm ci` in CI/CD for deterministic builds

### Added
- Generated `package-lock.json` for reproducible dependency installation
- Proper npm caching configuration using `cache-dependency-path`

### Changed
- All artifact upload/download actions now use v4 API
- Private key in CI/CD now properly quoted as string
- CI/CD now uses `npm ci` instead of `npm install` for faster, reliable builds

### Technical Details
- GitHub deprecated artifact actions v3 as of April 2024
- v4 provides improved performance and compatibility
- `npm ci` requires package-lock.json and ensures exact dependency versions
- See: https://github.blog/changelog/2024-04-16-deprecation-notice-v3-of-the-artifact-actions/

## [1.0.0] - 2024-09-29

### Added
- Initial release of Enterprise Asset Tokenization Platform
- Hyperledger Besu network with 4 validator nodes (IBFT 2.0)
- AssetToken.sol smart contract (ERC-20 with role-based access control)
- Java Spring Boot backend with Web3j integration
- PostgreSQL database with Flyway migrations
- JWT authentication and authorization
- Real-time blockchain event monitoring
- Docker Compose deployment configuration
- GitHub Actions CI/CD pipeline
- Comprehensive documentation (README, API docs, Architecture, Deployment guide)
- 13 REST API endpoints
- 35+ smart contract tests with 80%+ coverage
- Complete project structure with 52+ source files

### Security
- Role-based access control (ADMIN, MINTER, BURNER, PAUSER, INVESTOR, AUDITOR)
- BCrypt password hashing
- JWT token authentication
- OpenZeppelin security libraries
- Input validation on all endpoints
- Non-root Docker containers
