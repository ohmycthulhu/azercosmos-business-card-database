<?php


namespace App\Http\Controllers;


use App\BusinessCard;
use App\ChangeLog;
use App\Jobs\LoadImage;
use App\Note;
use GuzzleHttp\Client;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

class ChangeLogController extends Controller
{
    protected $change_log;
    public function __construct(ChangeLog $change_log)
    {
        parent::__construct($change_log, []);
        $this->change_log = $change_log;
    }

    public function synchronize(Request $request) {
        $changes = $request->input('changes');
        Log::debug("Got message - ".json_encode($request->all()));
        Log::debug("Files - ".json_encode($request->allFiles()));
        foreach ($request->allFiles() as $file_name => $file) {
            $file->move('public/'.$file_name);
        }
        foreach ($changes as $change) {
            $change = (array)json_decode($change);
            Log::debug("Change array is ".json_encode($change));
            switch ($change['type']) {
                case 'add':
                    $this->addCard(json_decode($change['data']));
                    break;
                case 'del':
                    $this->deleteCard($change['data']);
                    break;
                case 'upd':
                    $data = json_decode($change['data']);
                    $this->updateCard($data->id, $data->data);
                    break;
            }
        }
        return $changes;
    }

    private function addCard ($data) {
        $note = $data->note;
        unset($data->note);
        $data = (array)$data;
        $card = new BusinessCard();
        foreach ($data as $key => $value) {
            $card->{$key} = $value;
        }
        $card->save();
        if ($note) {
            $card->notes()->create([
                'note' => $note
            ]);
        }
    }

    private function deleteCard ($id) {
        BusinessCard::where('id', $id)->delete();
    }

    private function updateCard ($id, $data) {
        $data = (array)$data;
        if(array_has($data, 'note')) {
            $note = $data['note'];
            unset($data['note']);
            $noteRecord = Note::where('business_card_id', $id)->count();
            if ($noteRecord > 0) {
                Note::where('business_card_id', $id)->update(['note' => $note]);
            } else {
                if ($note) {
                    Note::create(['business_card_id' => $id, 'note' => $note]);
                }
            }
        }
        BusinessCard::where('id', $id)->update($data);
    }

    public function getChanges() {
        return ChangeLog::all();
    }

    public function launchSynchronization () {
        $changes =  ChangeLog::all()->toArray();
        $client = new Client();
        $form = array_map(function ($change) {
            if ($change['type'] == 'cim') {
                return [
                    'name' => $change['data'],
                    'contents' => fopen($change['data'], 'r')
                ];
            } else {
                return [
                    'name' => 'changes[]',
                    'contents' => json_encode($change)
                ];
            }
        }, $changes);

        $response = $client->request('POST', env('OTHER_SERVER_URL', '').'/synchronize', [
            'multipart' => $form
        ]);
        if ($response->getStatusCode() == 200) {
            ChangeLog::whereIn('id', array_map(function ($c) { return $c['id']; }, $changes))->delete();
        }
        $response = $client->request('GET', env('OTHER_SERVER_URL', '').'/synchronize');
        if ($response->getStatusCode() == 200) {
            $data = (string)$response->getBody();
            $changes = json_decode($data);
            foreach ($changes as $change) {
                switch ($change->type) {
                    case 'add':
                        $this->addCard(json_decode($change->data));
                        break;
                    case 'del':
                        $this->deleteCard($change->data);
                        break;
                    case 'upd':
                        $data = json_decode($change->data);
                        $this->updateCard($data->id, $data->data);
                        break;
                    case 'cim':
                        $this->dispatch(new LoadImage($change->data));
                        break;
                }
            }
        }
    }
}