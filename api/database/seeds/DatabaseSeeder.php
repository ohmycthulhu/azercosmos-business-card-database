<?php

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Run the database seeds.
     *
     * @return void
     */
    public function run()
    {
        // $this->call('UsersTableSeeder');
        $this->call(BusinessCardsTableSeeder::class);
        $this->call('PermissionsTableSeeder');
        $this->call(FillUsersPasswords::class);
    }
}
