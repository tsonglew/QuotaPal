import http.client
import json
from pathlib import Path
import ssl
import sys
import tempfile
import threading
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from upgrade_lab_server import certificates, make_server


class UpgradeLabServerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory()
        certificates(cls.directory.name)

    @classmethod
    def tearDownClass(cls):
        cls.directory.cleanup()

    def setUp(self):
        self.server, self.fixture = make_server(self.directory.name, 0)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.trust = ssl.create_default_context(cafile=str(Path(self.directory.name) / 'ca.pem'))

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=5)

    def request(self, host, method, path, body=None, headers=None):
        if host:
            connection = http.client.HTTPSConnection('127.0.0.1', self.server.server_port, context=self.trust, timeout=5)
            connection.set_tunnel(host, 443)
        else:
            connection = http.client.HTTPConnection('127.0.0.1', self.server.server_port, timeout=5)
        try:
            connection.request(method, path, body=body, headers=headers or {})
            response = connection.getresponse()
            return response.status, json.loads(response.read())
        finally:
            connection.close()

    def test_unknown_connect_never_forwards(self):
        connection = http.client.HTTPConnection('127.0.0.1', self.server.server_port, timeout=5)
        connection.set_tunnel('example.com', 443)
        try:
            with self.assertRaises(OSError):
                connection.request('GET', '/')
        finally:
            connection.close()
        self.assertEqual(1, self.fixture.counts['blocked'])

    def test_fixture_requires_explicit_ca_trust(self):
        connection = http.client.HTTPSConnection('127.0.0.1', self.server.server_port, timeout=5)
        connection.set_tunnel('auth.openai.com', 443)
        try:
            with self.assertRaises(ssl.SSLCertVerificationError):
                connection.request('POST', '/api/accounts/deviceauth/usercode', body='{}')
        finally:
            connection.close()
        self.assertEqual(0, self.fixture.counts['challenges'])

    def test_production_protocol_seed_and_rotated_credentials(self):
        status, _ = self.request('auth.openai.com', 'POST', '/api/accounts/deviceauth/usercode', '{}')
        self.assertEqual(200, status)
        status, _ = self.request('auth.openai.com', 'POST', '/api/accounts/deviceauth/token', '{}')
        self.assertEqual(200, status)
        status, session = self.request('auth.openai.com', 'POST', '/oauth/token',
                                       'code=synthetic-code&code_verifier=synthetic-verifier')
        self.assertEqual(200, status)
        headers = {'Authorization': 'Bearer ' + session['access_token'], 'ChatGPT-Account-Id': self.fixture.account}
        status, value = self.request('chatgpt.com', 'GET', '/backend-api/wham/usage', headers=headers)
        self.assertEqual(200, status)
        self.assertEqual(38, value['rate_limit']['primary_window']['used_percent'])
        self.request(None, 'POST', '/lab/after-upgrade')
        status, _ = self.request('chatgpt.com', 'GET', '/backend-api/wham/usage', headers=headers)
        self.assertEqual(401, status)
        status, renewed = self.request('auth.openai.com', 'POST', '/oauth/token',
                                      json.dumps({'grant_type': 'refresh_token', 'refresh_token': session['refresh_token']}),
                                      {'Content-Type': 'application/json'})
        self.assertEqual(200, status)
        self.assertNotEqual(session['access_token'], renewed['access_token'])
        self.assertNotEqual(session['refresh_token'], renewed['refresh_token'])
        headers['Authorization'] = 'Bearer ' + renewed['access_token']
        status, value = self.request('chatgpt.com', 'GET', '/backend-api/wham/usage', headers=headers)
        self.assertEqual(200, status)
        self.assertEqual(17, value['rate_limit']['primary_window']['used_percent'])
        self.assertEqual(3, self.fixture.counts['usage'])
        self.assertEqual(1, self.fixture.counts['renewals'])
        self.assertEqual(1, self.fixture.counts['rejected'])


if __name__ == '__main__':
    unittest.main()
