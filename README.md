# Periplus Selenium Test

Automated Java + Selenium + TestNG test for https://www.periplus.com/.

The test covers this scenario:

1. Open Google Chrome in a new window.
2. Navigate to Periplus.
3. Log in with a registered test account.
4. Search for one product.
5. Add one available product to the cart.
6. Verify the cart count increases.
7. Open the cart and verify the selected product appears there.

## Tech Stack

- Java
- Selenium WebDriver
- TestNG
- Gradle
- Google Chrome

## Project Structure

```text
.
├── build.gradle
├── settings.gradle
├── testng.xml
├── README.md
└── src/test/java/com/openway/periplus/PeriplusCartTest.java
```

## Prerequisites

Install or prepare:

- Java 17 or newer
- Gradle
- Google Chrome
- A registered and activated Periplus test account

Check Java and Gradle:

```bash
java -version
gradle -version
```

Selenium Manager is included through Selenium, so you usually do not need to install ChromeDriver manually.

## Account Setup

Create a test account manually at:

```text
https://www.periplus.com/account/Register
```

The registration page includes a CAPTCHA, so registration is not automated.

After registering, check your email and activate the account using the Periplus activation link. If the account is not activated, the test will fail during login with a message like:

```text
Warning: Your account with this email address has not been activated.
```

## Test Case Mapping

| Field | Details |
| --- | --- |
| Test Case ID | `TC_CART_001` |
| Test Scenario / Title | Verify that a logged-in Periplus user can search for a product and add it to the shopping cart. |
| Preconditions | User has a registered and activated Periplus account. Java 17 or newer is installed. Gradle is installed. Google Chrome is installed. Internet connection is available. |
| Test Data | `PERIPLUS_EMAIL`, `PERIPLUS_PASSWORD`, and product search keyword. Default keyword: `Meditations`. |
| Test Steps | 1. Open Google Chrome. 2. Navigate to `https://www.periplus.com/`. 3. Open the login page. 4. Enter email and password. 5. Submit login. 6. Search for a product. 7. Choose the first available product with an Add to cart button. 8. Add the product to the cart. 9. Open the cart page. |
| Expected Result | Login succeeds, cart count increases by one, and the selected product title appears in the shopping cart. |
| Actual Result | Generated after execution in the Gradle report: `build/reports/tests/test/index.html`. |
| Status | Pass or Fail based on the latest Gradle/TestNG execution result. |
| Priority / Severity | High priority / Critical severity because login, search, and add-to-cart are core e-commerce flows. |

## Environment Variables

Use environment variables for credentials. Do not hard-code your email or password in the Java test.

Recommended `.env` format:

```bash
export PERIPLUS_EMAIL='your-test-email@example.com'
export PERIPLUS_PASSWORD='your-test-password'
```

The `.env` file is ignored by Git through `.gitignore`.

Load the variables:

```bash
source .env
```

## How To Run

Run the test with a visible Chrome window:

```bash
source .env
gradle test
```

Or run it in one command:

```bash
PERIPLUS_EMAIL='your-test-email@example.com' \
PERIPLUS_PASSWORD='your-test-password' \
gradle test
```

## Run Headless

Headless mode runs Chrome without opening a visible browser window:

```bash
source .env
gradle test -Dheadless=true
```

## Change Product Search Keyword

The default product search keyword is `Meditations`.

To search for a different product:

```bash
source .env
gradle test -Dperiplus.productQuery='Atomic Habits'
```

## Optional System Properties

You can pass values directly with `-D`:

```bash
gradle test \
  -Dperiplus.email='your-test-email@example.com' \
  -Dperiplus.password='your-test-password' \
  -Dperiplus.productQuery='Meditations' \
  -Dheadless=true \
  -Dbrowser.timeout.seconds=30
```

Available options:

| Property | Environment variable | Default | Description |
| --- | --- | --- | --- |
| `periplus.email` | `PERIPLUS_EMAIL` | none | Periplus login email |
| `periplus.password` | `PERIPLUS_PASSWORD` | none | Periplus login password |
| `periplus.productQuery` | `PERIPLUS_PRODUCT_QUERY` | `Meditations` | Product keyword to search |
| `headless` | `HEADLESS` | `false` | Run Chrome in headless mode |
| `browser.timeout.seconds` | `BROWSER_TIMEOUT_SECONDS` | `25` | Selenium wait timeout |
| `chrome.binary` | `CHROME_BINARY` | auto-detected on macOS | Custom Chrome binary path |

## Test Reports

After running the test, open the Gradle HTML report:

```text
build/reports/tests/test/index.html
```

The raw XML result is here:

```text
build/test-results/test/TEST-com.openway.periplus.PeriplusCartTest.xml
```

## Troubleshooting

### Test is skipped

The credentials were not provided.

Fix:

```bash
source .env
gradle test
```

or pass credentials with `-Dperiplus.email` and `-Dperiplus.password`.

### Login failed

Try logging in manually at:

```text
https://www.periplus.com/account/Login
```

Common causes:

- Account has not been activated from email.
- Email or password is incorrect.
- Periplus is temporarily rejecting the login.

### Chrome does not open

Make sure Google Chrome is installed.

On macOS, the test auto-detects:

```text
/Applications/Google Chrome.app/Contents/MacOS/Google Chrome
```

If Chrome is installed somewhere else, pass:

```bash
gradle test -Dchrome.binary='/path/to/Google Chrome'
```

### Product is not added to cart

The test skips products marked `CURRENTLY UNAVAILABLE` and chooses the first available product with an `Add to cart` button.

If a search keyword has no available products, choose another keyword:

```bash
gradle test -Dperiplus.productQuery='Harry Potter'
```

## Notes

This is a live end-to-end test against the real Periplus website, so it can fail if the site changes its layout, has network issues, changes product availability, or blocks automated traffic.
