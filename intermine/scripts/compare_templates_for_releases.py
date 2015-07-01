#!/usr/bin/python

import sys
from time import ctime, time
import traceback
import smtplib
from email.mime.text import MIMEText
from collections import defaultdict
from intermine.webservice import Service

#### CONSTANTS ####

PROGRAM_START = time()

usage = """
%s: Compare templates from two versions of the same webservice

usage: %s www.flymine.org/query beta.flymine.org/beta ["email@to"] ["email@from"]

All arguments are positional. The last two are optional.

Arguments:
    * service version A
    * service version B (optional - service to compare to)
    * an email address to send email to (optional - print to std out if not present)
    * an email to mark as the sender (optional - defaults to the first email address)

"""

SUBJECT = "The results of comparison between %s and %s at %s" 
BODY = """
Template comparison run complete. 

The template comparison run you requested at {initial_time}
between {rel_a} and {rel_b} has been completed at {time}
(taking {duration:.2f} seconds).

The results are as follows:

"""

rfc822_specials = '()<>@,;:\\"[]'

#### ROUTINES ####

def compare_templates(url_a, url_b=None, send_to=None, send_from=None):
    """Main program logic"""

    if url_b is None:
        url_b = url_a
    elif isAddressValid(url_b):
        send_to = url_b
        url_b = url_a

    if send_from is None:
        send_from = send_to
    if send_to is not None:
        if not isAddressValid(send_to) or not isAddressValid(send_from):
            raise Exception("Invalid email addresses: '%s', '%s'" % (send_to, send_from))

    results = fetch_results(url_a, url_b)
    report_results(results, send_to, send_from)

def fetch_results(url_a, url_b):
    try: 
        services = map(Service, [url_a, url_b])
    except Exception as e:
        raise Exception("Invalid service urls: '%s', '%s'\n" % (url_a, url_b) + str(e))

    results = {
        "failures_from": dict(( (service.release, {}) for service in services)),
        "rows_from": dict(( (service.release, {}) for service in services))
    }

    start = time()

    queried = set()

    for service in services:
        if service.release in queried:
            continue
        else:
            queried.add(service.release)

        for name in service.templates.keys():
            try:
                template = service.get_template(name)
            except Exception as e:
                results["failures_from"][service.release][name] = str(e) + "\nXML:\n" + str(service.templates[name])

            print "Querying %s for results for %s" % (service.release, name)
            try: 
                c = template.count()
                results["rows_from"][service.release][name] = c
            except Exception as e:
                results["failures_from"][service.release][name] = str(e)

    end = time()
    total = end - start
    print "Finished fetching results: that took %d min, %d secs" % (total / 60, total % 60)
    return results

def report_results(results, send_to, send_from):
    """Handle the results"""
    body = create_message_body(results)
    print body
    if send_to is not None:
        print "Sending email to %s" % send_to
        msg = MIMEText(body)
        params = results["rows_from"].keys()
        if len(params) == 1:
            params.append("itself")
        params.append(ctime(time()))
        msg['Subject'] = SUBJECT % tuple(params)
        msg['From'] = send_from
        msg['To'] = send_to
        smtp = smtplib.SMTP('localhost')
        smtp.sendmail(send_from, [send_to], msg.as_string())
        smtp.quit()

def create_message_body(results):
    """Analyse the data and present it as a string"""
    releases = results["rows_from"].keys()
    if len(releases) == 1:
        rel_a = releases[0]
        rel_b = rel_a
    else:
        rel_a, rel_b = releases

    body_params = {
        "rel_a": rel_a,
        "rel_b": rel_b,
        "initial_time": ctime(PROGRAM_START),
        "time": ctime(time()),
        "duration": time() - PROGRAM_START
    }

    body = BODY.format(**body_params)

    body += "\nFAILURES:\n"
    failures_from = results['failures_from']
    for rel, failures in failures_from.items():
        if len(failures):
            body += (rel + "\n").ljust(80, "=")  + "\n"
            for name, reason in failures.items():
                body += "%s: %s\n" % (name, reason)

    successes_from = results["rows_from"]
    template_results = {}
    longest_template_name = 0
    for rel, successes in successes_from.items():
        for name, count in successes.items():
            if name not in template_results:
                template_results[name] = defaultdict(lambda: 0) 
            if len(name) > longest_template_name:
                longest_template_name = len(name)
            template_results[name][rel] = count

    if len(releases) > 1:
        body += "\nBY TEMPLATE:\n"

        fmt = "%-" + str(longest_template_name) + "s | %-6s | %-6s | %s\n" 
        body += fmt % ("NAME", rel_a, rel_b, "CATEGORY")
        body += "".ljust(100, "-") + "\n"

        fmt = "%-" + str(longest_template_name) + "s | %6d | %6d | %s\n" 
        for name, results_by_rel in sorted(template_results.items()):
            diff = abs(reduce(lambda x, y: x - y, results_by_rel.values()))
            max_c = max(results_by_rel.values())

            if diff == 0:
                category = "SAME"
            else: 
                proportion = float(diff) / float(max_c)
                if proportion < 0.1:
                    category = "CLOSE"
                elif proportion < 0.5:
                    category = "DIFFERENT"
                else: 
                    category = "VERY DIFFERENT"

            body += fmt % (name, results_by_rel[rel_a], results_by_rel[rel_b], category)

    body += "\nALL SUCCESSES:\n"
    fmt = "%-" + str(longest_template_name) + "s | %6d\n"
    for rel, successes in successes_from.items():
        body += "\n" + (rel + "\n").ljust(80, "=")  + "\n"
        for name, count in sorted(successes.items()):
            body += fmt % (name, count)

    return body

def isAddressValid(addr):
    """Check that Email addresses are valid"""
    # First we validate the name portion (name@domain)
    c = 0
    while c < len(addr):
        if addr[c] == '"' and (not c or addr[c - 1] == '.' or addr[c - 1] == '"'):
            c = c + 1
            while c < len(addr):
                if addr[c] == '"': break
                if addr[c] == '\\' and addr[c + 1] == ' ':
                    c = c + 2
                    continue
                if ord(addr[c]) < 32 or ord(addr[c]) >= 127: return 0
                c = c + 1
            else: return 0
            if addr[c] == '@': break
            if addr[c] != '.': return 0
            c = c + 1
            continue
        if addr[c] == '@': break
        if ord(addr[c]) <= 32 or ord(addr[c]) >= 127: return 0
        if addr[c] in rfc822_specials: return 0
        c = c + 1
    if not c or addr[c - 1] == '.': return 0

    # Next we validate the domain portion (name@domain)
    domain = c = c + 1
    if domain >= len(addr): return 0
    count = 0
    while c < len(addr):
        if addr[c] == '.':
            if c == domain or addr[c - 1] == '.': return 0
            count = count + 1
        if ord(addr[c]) <= 32 or ord(addr[c]) >= 127: return 0
        if addr[c] in rfc822_specials: return 0
        c = c + 1

    return count >= 1

#### CALL MAIN ####

if __name__ == "__main__":

    args = sys.argv[1:]

    try: 
        compare_templates(*args)
    except:
        tb = traceback.format_exc()
        print tb
        print usage % (sys.argv[0], sys.argv[0])
        exit(1)

