import requests
import json
import os
import uuid

DEFAULT_BASE = 'http://localhost:8080/intermine-demo'

class TemporaryUser:
    """A helper class for creating new users on a target webservice"""

    def __init__(self, name=None, password=None):
        self.base_url = os.getenv('TESTMODEL_BASE', DEFAULT_BASE)
        self.name = name if name is not None else '__TEMPORARY_USER__'
        self.password = password if password is not None else 'password'

    def create(self):
        """Create the new user - SHOULD BE CALLED IN SETUP"""

        try:
            self.delete()
        except:
            pass

        methodURI = '/service/users'
        payload = {'name': self.name, 'password': self.password}

        result = requests.post(self.base_url + methodURI, data=payload)
        j = result.json()

        if j['statusCode'] == 200:
            print "User created successfully: " + self.name
        else:
            print "Error creating user: " + j['error']

    def _get_deregistration_token(self):
        """Get the deregistration token for this user"""
        methodURI = '/service/user/deregistration'
        result = requests.post(self.base_url + methodURI, auth=(self.name, self.password))
        j = result.json()
        return j['token']['uuid']

    def delete(self):
        """Delete this user from the webservice - YOU MUST CALL THIS IN tearDown!!"""
        methodURI = '/service/user'
        payload = {'deregistrationToken': self._get_deregistration_token()}
        requests.delete(self.base_url + methodURI, params=payload, auth=(self.name, self.password))

