<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$var1 = $_GET['nodeid'];
$var2 = $_GET['datetime'];

$command = escapeshellcmd("python3 /mnt/c/www/graph.py " . $var1 . " " . $var2);
$output = shell_exec($command);
echo "<pre>$output</pre>";
?>