import requests
import json
import os

DEFAULT_BASE = 'http://localhost:8080/intermine-demo'

class IMUser:

	def __init__(self, name, password=None):
		self.base_url = os.getenv('TESTMODEL_BASE', DEFAULT_BASE)

		if password is None:
			self.create(name, "password")
		else:
			self.create(name, password)

	def create(self, name, password):

		self.name = name
		self.password = password

		try:
			self.delete()
		except:
			pass

		methodURI = '/service/users'
		payload = {'name': name, 'password': password}

		result = requests.post(self.base_url + methodURI, data=payload)
		j = result.json()

		if j['statusCode'] == 200:
			print "User created successfully: " + self.name
		else:
			print "Error creating user: " + j['error']

	def get_deregistration_token(self):

		methodURI = '/service/user/deregistration'
		result = requests.post(self.base_url + methodURI, auth=(self.name, self.password))
		j = result.json()
		return j['token']['uuid']

	def delete(self):

		methodURI = '/service/user'
		payload = {'deregistrationToken': self.get_deregistration_token()}
		response = requests.delete(self.base_url + methodURI, params=payload, auth=(self.name, self.password))