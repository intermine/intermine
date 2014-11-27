#!/usr/bin/env python

from __future__ import print_function

import os
import sys
import requests

base_url = os.getenv('TESTMODEL_URL', 'http://localhost:8080/intermine-demo')
resource = base_url + '/service/user'

token = sys.stdin.read()

headers = {'Accept': 'application/json','Authorization': 'Bearer ' + token}

print("Requesting resource: ", resource)
resp = requests.get(resource, headers = headers)

if resp.status_code != 200:
    print(resp.text)
    sys.exit(resp.status_code)

data = resp.json()

print('Authorized as', data['user'])

