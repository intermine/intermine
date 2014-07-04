#!/usr/bin/env python
import requests
import json
import config

class IMUser:

	# base_url = 'http://beta.flymine.org/beta/'
	base_url = config.base_api_url

	def __init__(self, name, password=None):
		name = name.encode("utf-8")
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

		methodURI = 'service/users'
		payload = {'name': name, 'password': password}

		result = requests.post(self.base_url + methodURI, data=payload)
		j = result.json()

		if j['statusCode'] == 200:
			print "User created successfully: " + self.name
		else:
			print "Error creating user: " + j['error']

	def get_deregistration_token(self):

		methodURI = 'service/user/deregistration'
		result = requests.post(self.base_url + methodURI, auth=(self.name, self.password))
		j = result.json()
		return j['token']['uuid']

	def delete(self):

		methodURI = 'service/user'
		payload = {'deregistrationToken': self.getDeregistrationToken()}
		response = requests.delete(self.base_url + methodURI, params=payload, auth=(self.name, self.password))
