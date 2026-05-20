# Periplus Selenium Test

Automated Java + Selenium + TestNG test for https://www.periplus.com/.

The test covers this scenario:

1. Open Google Chrome in a new window.
2. Navigate to Periplus.
3. Log in with a registered test account.
4. Clear the cart so the scenario is isolated.
5. Search for one available product.
6. Add the product to the cart.
7. Increase the cart quantity by the configured requested quantity.
8. Verify the cart product id, ISBN, title, quantity, unit price, subtotal, total, and header count.
9. Continue to checkout and verify the checkout flow opens for the next step.
10. Restore the touched cart item to its original quantity after the test.

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
└── src/test/java/com/openway/periplus
    ├── PeriplusCartTest.java
    ├── config
    ├── core
    ├── model
    └── pages
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
| Test Scenario / Title | Verify that a logged-in Periplus user can search for a product, add multiple units to the shopping cart, validate cart totals, and continue to checkout. |
| Preconditions | User has a registered and activated Periplus account. Java 17 or newer is installed. Gradle is installed. Google Chrome is installed. Internet connection is available. |
| Test Data | `PERIPLUS_EMAIL`, `PERIPLUS_PASSWORD`, product search keyword, optional product id / ISBN, and requested cart quantity. Default keyword: `Harry Potter`; default requested quantity: `2`. |
| Test Steps | 1. Open Google Chrome. 2. Navigate to `https://www.periplus.com/`. 3. Open the login page. 4. Enter email and password. 5. Submit login. 6. Open the configured product detail page when product id / ISBN are provided, otherwise search and choose the first available product. 7. Capture the existing cart count, subtotal, and current quantity for the selected product if it already exists in the cart. 8. Add the product to the cart from the product detail page. 9. Open the cart page. 10. Update the product line quantity to existing quantity plus requested quantity. 11. Continue to checkout. 12. Restore the touched cart item to its original quantity during teardown. |
| Expected Result | Login succeeds, selected product id and ISBN match from product detail to cart, product quantity increases by the requested quantity, cart subtotal/total increase by unit price multiplied by requested quantity, header cart count increases by requested quantity, checkout opens for the next checkout step, and the test does not leave the selected cart item changed after execution. |
| Actual Result | Generated after execution in the Gradle report: `build/reports/tests/test/index.html`. |
| Status | Pass or Fail based on the latest Gradle/TestNG execution result. |
| Priority / Severity | High priority / Critical severity because login, search, and add-to-cart are core e-commerce flows. |

## Environment Variables

Use environment variables for credentials. Do not hard-code your email or password in the Java test.

Recommended `.env` format:

```bash
export PERIPLUS_EMAIL='your-test-email@example.com'
export PERIPLUS_PASSWORD='your-test-password'
export PERIPLUS_BASE_URL='https://www.periplus.com/'
export PERIPLUS_PRODUCT_QUERY='Harry Potter'
export PERIPLUS_PRODUCT_ID='66846208'
export PERIPLUS_PRODUCT_ISBN='9798886633849'
export PERIPLUS_CART_QUANTITY='2'
export HEADLESS='false'
export BROWSER_TIMEOUT_SECONDS='25'
export CHROME_BINARY=''
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

The default product search keyword is `Harry Potter`.

To search for a different product:

```bash
source .env
gradle test -Dperiplus.productQuery='Atomic Habits'
```

## Pin A Specific Product

For more deterministic runs, pass a Periplus product id, ISBN, or both. The search keyword must still return that product.

```bash
source .env
gradle test \
  -Dperiplus.productQuery='Harry Potter' \
  -Dperiplus.productId='66846208' \
  -Dperiplus.productIsbn='9798886633849'
```

## Change Cart Quantity

The default quantity is `2`, so the scenario proves more than one unit can be added.

```bash
source .env
gradle test -Dperiplus.cartQuantity=3
```

## Optional System Properties

You can pass values directly with `-D`:

```bash
gradle test \
  -Dperiplus.email='your-test-email@example.com' \
  -Dperiplus.password='your-test-password' \
  -Dperiplus.productQuery='Harry Potter' \
  -Dperiplus.productId='66846208' \
  -Dperiplus.productIsbn='9798886633849' \
  -Dperiplus.cartQuantity=2 \
  -Dheadless=true \
  -Dbrowser.timeout.seconds=30
```

Available options:

| Property | Environment variable | Default | Description |
| --- | --- | --- | --- |
| `periplus.email` | `PERIPLUS_EMAIL` | none | Periplus login email |
| `periplus.password` | `PERIPLUS_PASSWORD` | none | Periplus login password |
| `periplus.baseUrl` | `PERIPLUS_BASE_URL` | `https://www.periplus.com/` | Periplus base URL |
| `periplus.productQuery` | `PERIPLUS_PRODUCT_QUERY` | `Harry Potter` | Product keyword used when product id / ISBN are not pinned |
| `periplus.productId` | `PERIPLUS_PRODUCT_ID` | none | Optional Periplus product id to pin the selected product |
| `periplus.productIsbn` | `PERIPLUS_PRODUCT_ISBN` | none | Optional ISBN to open and pin the selected product detail page |
| `periplus.cartQuantity` | `PERIPLUS_CART_QUANTITY` | `2` | Requested quantity to add. Values below `1` are raised to `1`. |
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

Failure screenshots are saved here when Selenium can capture the browser:

```text
build/reports/screenshots/
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

When product id / ISBN are configured, the test opens that product detail page directly. Without a pinned product, it skips products marked `CURRENTLY UNAVAILABLE` and chooses the first available product with an `Add to cart` button.

If a search keyword has no available products, choose another keyword:

```bash
gradle test -Dperiplus.productQuery='Harry Potter'
```

## Notes

This is a live end-to-end test against the real Periplus website, so it can fail if the site changes its layout, has network issues, changes product availability, or blocks automated traffic.
