"""Inspect a VNPay 2.1 payment URL offline. Never reads .env or sends requests."""
import argparse
import getpass
import hashlib
import hmac
import ipaddress
import json
import re
import sys
from pathlib import Path
from urllib.parse import parse_qsl, quote_plus, urlsplit


def inspect_url(value):
    """Return decoded fields and non-sensitive diagnostics; reject ambiguous input."""
    value = value.strip()
    if re.search(r"[\s\x00-\x1f\x7f]", value):
        raise ValueError("URL contains unencoded whitespace/control characters.")
    if re.search(r"%(?![0-9a-fA-F]{2})", value):
        raise ValueError("URL contains malformed percent encoding.")
    url = urlsplit(value)
    if url.scheme != "https" or not url.hostname or url.username or url.password or url.fragment:
        raise ValueError("Expected a complete HTTPS payment URL without credentials or fragment.")
    pairs = parse_qsl(url.query, keep_blank_values=True, strict_parsing=True, errors="strict")
    fields = {}
    for key, item in pairs:
        if not re.fullmatch(r"vnp_[A-Za-z0-9_]+", key):
            raise ValueError("Unexpected parameter name; use the payment URL, not the error page URL.")
        if key in fields:
            raise ValueError("Duplicate parameters: URL is ambiguous.")
        fields[key] = item
    required = ("vnp_Version", "vnp_Command", "vnp_TmnCode", "vnp_Amount",
                "vnp_CurrCode", "vnp_TxnRef", "vnp_OrderInfo", "vnp_OrderType",
                "vnp_ReturnUrl", "vnp_IpAddr", "vnp_CreateDate", "vnp_SecureHash")
    if any(not fields.get(key) for key in required):
        raise ValueError("Missing/empty required payment parameters; use the original payment URL.")
    if fields["vnp_Version"] != "2.1.0" or fields["vnp_Command"] != "pay":
        raise ValueError("Only VNPay 2.1.0 pay URLs are supported.")
    if not re.fullmatch(r"[a-fA-F0-9]{128}", fields["vnp_SecureHash"]):
        raise ValueError("vnp_SecureHash must contain 128 hexadecimal characters.")
    try:
        ipaddress.ip_address(fields["vnp_IpAddr"])
        valid_ip = True
    except ValueError:
        valid_ip = False
    return_url = urlsplit(fields["vnp_ReturnUrl"])
    report = {
        "scope": "OFFLINE_URL_DIAGNOSTIC_NOT_GATEWAY_ACCEPTANCE",
        "networkRequests": 0,
        "gatewayHost": url.hostname,
        "sandboxEndpoint": (url.hostname == "sandbox.vnpayment.vn"
                            and url.path == "/paymentv2/vpcpay.html"),
        "merchantHasEdgeWhitespace": fields["vnp_TmnCode"] != fields["vnp_TmnCode"].strip(),
        "amountFormatValid": bool(re.fullmatch(r"[0-9]{1,12}", fields["vnp_Amount"])),
        "currencyVnd": fields["vnp_CurrCode"] == "VND",
        "ipFormatValid": valid_ip,
        "returnUrlMatchesScmsBackend": fields["vnp_ReturnUrl"] ==
            "https://scms-app-p1gy.onrender.com/api/payments/vnpay/return",
        "returnUrlHttps": return_url.scheme == "https" and bool(return_url.hostname),
        "emptyParameterCount": sum(not item for item in fields.values()),
    }
    return fields, report


def canonical_data(fields):
    # Java URLEncoder's form encoding: space -> '+', '*' unchanged, '~' -> '%7E'.
    def encode(value):
        return quote_plus(value, safe="*", encoding="utf-8").replace("~", "%7E")
    return "&".join(encode(key) + "=" + encode(value)
                    for key, value in sorted(fields.items())
                    if key not in ("vnp_SecureHash", "vnp_SecureHashType") and value)


def matches_secret(fields, secret):
    expected = hmac.new(secret.encode("utf-8"), canonical_data(fields).encode("utf-8"),
                        hashlib.sha512).hexdigest()
    return hmac.compare_digest(expected, fields["vnp_SecureHash"].lower())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url-file", type=Path, help="Local file containing only the original payment URL")
    parser.add_argument("--compare-browser", action="store_true", help="Prompt for the first VNPay request URL")
    parser.add_argument("--check-secret", action="store_true", help="Prompt privately for the VNPay-issued key")
    args = parser.parse_args()
    if args.check_secret and not sys.stdin.isatty():
        parser.error("Run --check-secret interactively in a terminal; do not pipe the key.")
    try:
        raw = (args.url_file.read_text(encoding="utf-8-sig").strip() if args.url_file else
               input("Payment URL from backend (paste one line): ").strip())
        fields, report = inspect_url(raw)
        if args.compare_browser:
            browser_raw = input("First VNPay request URL from Network (not Error.html): ").strip()
            browser_fields, _ = inspect_url(browser_raw)
            report["browserComparison"] = {
                "rawUrlsEqual": raw == browser_raw,
                "gatewayTargetEqual": urlsplit(raw)[:3] == urlsplit(browser_raw)[:3],
                "decodedFieldsEqual": fields == browser_fields,
                "canonicalDataEqual": canonical_data(fields) == canonical_data(browser_fields),
                "changedParameters": sorted(key for key in fields.keys() | browser_fields.keys()
                                            if fields.get(key) != browser_fields.get(key)),
            }
        if args.check_secret:
            merchant = input("VNPay-issued TmnCode (Enter to skip): ")
            if merchant:
                report["merchantMatchesIssuedCode"] = merchant == fields["vnp_TmnCode"]
            secret = getpass.getpass("VNPay-issued HashSecret (hidden; never saved): ")
            if not secret:
                raise ValueError("No secret entered; signature was not checked.")
            report["enteredSecretHasEdgeWhitespace"] = secret != secret.strip()
            report["signatureMatchesEnteredSecret"] = matches_secret(fields, secret)
            if secret != secret.strip():
                report["signatureMatchesTrimmedSecret"] = matches_secret(fields, secret.strip())
        print(json.dumps(report, ensure_ascii=True, indent=2))
        return 0
    except (ValueError, OSError, UnicodeError, EOFError):
        # Never echo user input, URLs, secret values, or exception strings.
        print("Cannot inspect input: use the full, unmodified HTTPS vpcpay.html URL, "
              "with unique vnp_* parameters and a 128-character signature.", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
