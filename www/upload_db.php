<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

$file_path = "./files/";

if (!file_exists($file_path)) {
    mkdir($file_path, 0777, true);
}
//var_dump($_FILES);
$uploaded = true;
foreach ($_FILES as $key => $val) {
    $file = $file_path . basename($val['name']);
    $uploaded = move_uploaded_file($val['tmp_name'], $file);
}

echo $uploaded ? "Uploaded" : "Failed";
