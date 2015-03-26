<?php

$form_errors = array ();

if (!empty ($_POST)) {
	if (empty ($_POST['name']))
		$form_errors['name'] = 'Please Enter your Name';

	if (empty ($_POST['comments']))
		$form_errors['comments'] = 'Please enter a message';

	if (empty ($_POST['email']) || !eregi("^[_a-z0-9-]+(\.[_a-z0-9-]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,})$", trim($_POST['email']))) {
		$form_errors['email'] = 'Please enter a valid email address.';
	}

	if (empty ($form_errors)) {

        function SendEmail($to, $subject, $body, $from = false, $headers="", $bcc = false) {
	      if($from !== false) {
		    $headers .= "From: $from\n";
	      }
	      if ($bcc) $headers .= "BCC: ".$bcc."\n";
	      $headers .= 'MIME-Version: 1.0' . "\n";
	      $headers .= 'Content-type: text/html; charset=iso-8859-1' . "\n";
	      @mail($to, $subject, $body, $headers);
        }

		$emailmsg =<<<END_OF_EMAIL
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>canvasXpress Mail Inquiry</title>
</head>
<body style="font-family:Arial, Helvetica, sans-serif; padding:0px; margin:10px; color:#666">
	<table width="100%" border="0">
		<tr>
			<th colspan="2" align="left"><h3 style="color:#666;">canvasXpress Website Mail:</h3></th>
		</tr>
		<tr>
			<th colspan="2" align="left"><hr></th>
			</tr>
		<tr>
			<th width="130" align="left">&nbsp;</th>
			<td width="823">&nbsp;</td>
		</tr>
		<tr>
			<th align="left">Name:</th>
			<td>$_POST[name]</td>
		</tr>
		<tr>
			<th align="left">Email:</th>
			<td><a href="mailto:$_POST[email]">$_POST[email]</a></td>
		</tr>
		<tr>
			<th align="left">Comments:</th>
			<td>$_POST[comments]</td>
		</tr>
	</table>
</body>
</html>
END_OF_EMAIL;

		SendEmail("imnphd@gmail.com", "canvasXpress.org | Website Mail", $emailmsg, "WebMail <info@canvasXpress.org>", "", "client.forms@artician.net");
		echo '<div id="qc_success" class="success">Your message has been sent successfully.</div>';
		exit;

	}
}

if (!empty ($form_errors)) {
	echo '<ul class="errors">
		<strong>Please Correct the Following:</strong>';
	foreach ($form_errors AS $error) {
		echo '<li>' . $error . '</li>';
	}
	echo '</ul>';
}
?>